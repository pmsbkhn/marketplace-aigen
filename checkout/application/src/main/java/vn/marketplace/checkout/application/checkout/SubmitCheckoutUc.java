package vn.marketplace.checkout.application.checkout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.workflow.CompensatingWorkflow;
import tech.vsf.ptnt.msfw.domain.workflow.WorkflowContext;
import tech.vsf.ptnt.msfw.domain.workflow.WorkflowObserver;
import vn.marketplace.checkout.application.checkout.CatalogPort.SkuPriceDto;
import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;
import vn.marketplace.checkout.application.checkout.InventoryPort.ReservationDto;
import vn.marketplace.checkout.application.checkout.InventoryPort.ReserveItemDto;
import vn.marketplace.checkout.application.checkout.OrderPort.CreateOrderDto;
import vn.marketplace.checkout.application.checkout.OrderPort.OrderLineDto;
import vn.marketplace.checkout.application.checkout.OrderPort.ShippingAddressDto;
import vn.marketplace.checkout.application.checkout.PaymentPort.EscrowAllocationDto;
import vn.marketplace.checkout.application.checkout.PaymentPort.EscrowDto;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd.CheckoutItemInput;
import vn.marketplace.checkout.domain.checkout.Currency;
import tech.vsf.ptnt.msfw.domain.core.IdempotencyKey;
import vn.marketplace.checkout.domain.checkout.Money;
import vn.marketplace.checkout.domain.checkout.management.CartSnapshot;
import vn.marketplace.checkout.domain.checkout.management.CheckoutSaga;
import vn.marketplace.checkout.domain.checkout.management.LineItem;
import vn.marketplace.checkout.domain.checkout.management.MerchantGroup;
import vn.marketplace.checkout.domain.checkout.management.OrderSplitter;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

/**
 * The checkout saga orchestrator:
 * <ol>
 *   <li><b>Idempotency</b> — a cached REDIRECTED session returns immediately (no second saga); a
 *       held lock means a concurrent duplicate → {@code CHECKOUT_IN_PROGRESS} (TC-CHK-04);</li>
 *   <li><b>PRICING</b> — prices fetched from Catalog only; client prices do not exist in the
 *       command (TC-CHK-03); the client-claimed merchant is cross-checked against Catalog;</li>
 *   <li><b>RESERVING</b> — all-or-nothing stock hold; a shortage fails fast (nothing held,
 *       nothing to compensate, NOT cached so a restock retry can succeed);</li>
 *   <li><b>ORDERING</b> — one pending order per merchant group ({@code checkoutRef = key:merchantId});</li>
 *   <li><b>ESCROWING</b> — one whole-cart escrow with per-order allocations;</li>
 *   <li><b>Compensation</b> — on any failure after the reserve: cancel created orders FIRST
 *       (newest first), release the reservation LAST — strict reverse order (TC-CHK-02); the
 *       failure is cached so the client knows to retry with a fresh key.</li>
 * </ol>
 * The step sequence and the reverse-order compensation run on msfw's
 * {@link CompensatingWorkflow}: each step's compensation is declared next to it, a failing
 * compensation never stops the remaining ones (it rides the original failure as a suppressed
 * exception → the COMPENSATION INCOMPLETE path), and runs are observable as
 * {@code msfw.workflow.runs} when a {@link WorkflowObserver} is wired.
 *
 * <p>No {@code @EventPublishHandler}: Checkout persists nothing through an msfw Repository and
 * publishes no domain events — its durable effects live in the downstream contexts.</p>
 */
public class SubmitCheckoutUc implements SubmitCheckout {

    private final CatalogPort catalog;
    private final InventoryPort inventory;
    private final OrderPort order;
    private final PaymentPort payment;
    private final CheckoutSessionPort sessions;
    private final OrderSplitter splitter = new OrderSplitter();
    private final int maxItemsPerCart;
    private final int maxMerchantsPerCart;
    private final CompensatingWorkflow workflow;

    public SubmitCheckoutUc(CatalogPort catalog, InventoryPort inventory, OrderPort order,
                            PaymentPort payment, CheckoutSessionPort sessions,
                            int maxItemsPerCart, int maxMerchantsPerCart) {
        this(catalog, inventory, order, payment, sessions, maxItemsPerCart, maxMerchantsPerCart,
                WorkflowObserver.NOOP);
    }

    public SubmitCheckoutUc(CatalogPort catalog, InventoryPort inventory, OrderPort order,
                            PaymentPort payment, CheckoutSessionPort sessions,
                            int maxItemsPerCart, int maxMerchantsPerCart,
                            WorkflowObserver workflowObserver) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.order = order;
        this.payment = payment;
        this.sessions = sessions;
        this.maxItemsPerCart = maxItemsPerCart;
        this.maxMerchantsPerCart = maxMerchantsPerCart;
        this.workflow = buildWorkflow(workflowObserver);
    }

    @Override
    public CheckoutResultView execute(SubmitCheckoutCmd cmd) {
        validateShape(cmd);

        // Idempotency fast-path: a terminal session answers without re-running the saga.
        Optional<CheckoutSession> cached = sessions.find(cmd.idempotencyKey());
        if (cached.isPresent()) {
            return resultFromCache(cached.get());
        }

        if (!sessions.tryLock(cmd.idempotencyKey())) {
            throw new CheckoutDomainException(CheckoutErrorCode.CHECKOUT_IN_PROGRESS);
        }
        try {
            return runSaga(cmd);
        } finally {
            sessions.unlock(cmd.idempotencyKey());
        }
    }

    private CheckoutResultView runSaga(SubmitCheckoutCmd cmd) {
        CheckoutSaga saga = CheckoutSaga.start(new IdempotencyKey(cmd.idempotencyKey()), cmd.buyerId());

        // ---- PRICING: Catalog is the only price authority (TC-CHK-03); no side effects yet ----
        CartSnapshot cart = priceCart(cmd);
        List<MerchantGroup> groups = splitter.split(cart);
        if (groups.size() > maxMerchantsPerCart) {
            throw new CheckoutDomainException(CheckoutErrorCode.TOO_MANY_MERCHANTS);
        }

        try {
            WorkflowContext result = workflow.execute(ctx -> {
                ctx.put("cmd", cmd).put("saga", saga).put("cart", cart).put("groups", groups);
            });
            List<String> orderIds = result.require("orderIds");
            EscrowDto escrow = result.require("escrow");
            return new CheckoutResultView(escrow.paymentUrl(), orderIds, cart.grandTotal().amount());

        } catch (RuntimeException stepFailure) {
            if (isOutOfStock(stepFailure)) {
                // All-or-nothing reserve held nothing; NOT cached so a restock retry can succeed.
                throw stepFailure;
            }
            if (stepFailure.getSuppressed().length > 0) {
                // A compensation failed too — ghost order/reservation: reconciliation runbook,
                // and NOT cached (the state is unknown, a cached failure could lie).
                throw new CheckoutDomainException(CheckoutErrorCode.CHECKOUT_FAILED,
                        "COMPENSATION INCOMPLETE — manual reconciliation required: "
                                + suppressedMessages(stepFailure));
            }
            saga.fail(stepFailure.getMessage());
            sessions.saveFailed(cmd.idempotencyKey(), cmd.buyerId(), stepFailure.getMessage());
            throw new CheckoutDomainException(CheckoutErrorCode.CHECKOUT_FAILED,
                    "Checkout failed and was compensated — retry with a new idempotency key. Cause: "
                            + stepFailure.getMessage());
        }
    }

    /**
     * RESERVING → ORDERING → ESCROWING → session save, with TC-CHK-02 compensation declared
     * step-by-step: a later failure cancels the pending orders FIRST (newest first — the
     * create-orders compensation), releases the reservation LAST (the reserve-stock one).
     */
    private CompensatingWorkflow buildWorkflow(WorkflowObserver observer) {
        return CompensatingWorkflow.named("checkout")
                .observer(observer)
                .step("reserve-stock", ctx -> {
                    SubmitCheckoutCmd cmd = ctx.require("cmd");
                    CheckoutSaga saga = ctx.require("saga");
                    CartSnapshot cart = ctx.require("cart");
                    saga.markReserving();
                    ReservationDto reservation =
                            inventory.reserveStock(cmd.idempotencyKey(), aggregateBySku(cart));
                    if (!reservation.allReserved()) {
                        saga.fail("OUT_OF_STOCK");
                        throw new CheckoutDomainException(CheckoutErrorCode.OUT_OF_STOCK,
                                "Out of stock: " + reservation.shortages());
                    }
                    ctx.put("reservation", reservation);
                }, ctx -> {
                    ReservationDto reservation = ctx.require("reservation");
                    inventory.releaseStock(reservation.reservationId());
                })
                .step("create-orders", ctx -> {
                    SubmitCheckoutCmd cmd = ctx.require("cmd");
                    CheckoutSaga saga = ctx.require("saga");
                    List<MerchantGroup> groups = ctx.require("groups");
                    saga.markOrdering();
                    List<String> createdOrderIds = new ArrayList<>();
                    Map<String, String> orderIdByMerchant = new LinkedHashMap<>();
                    ctx.put("orderIds", createdOrderIds).put("orderIdByMerchant", orderIdByMerchant);
                    try {
                        for (MerchantGroup group : groups) {
                            String orderId = order.createPendingOrder(toCreateOrder(cmd, group));
                            createdOrderIds.add(orderId);
                            orderIdByMerchant.put(group.merchantId(), orderId);
                        }
                    } catch (RuntimeException creationFailure) {
                        // The step is its own atomicity boundary: undo the partial creations here
                        // (a failed step's declared compensation does not run).
                        cancelNewestFirst(createdOrderIds, creationFailure);
                        throw creationFailure;
                    }
                }, ctx -> {
                    List<String> createdOrderIds = ctx.require("orderIds");
                    cancelNewestFirst(createdOrderIds, null);
                })
                // No escrow-cancel API exists: like before, a failure AFTER initEscrow compensates
                // orders + reservation only.
                .step("init-escrow", ctx -> {
                    SubmitCheckoutCmd cmd = ctx.require("cmd");
                    CheckoutSaga saga = ctx.require("saga");
                    CartSnapshot cart = ctx.require("cart");
                    List<MerchantGroup> groups = ctx.require("groups");
                    List<String> createdOrderIds = ctx.require("orderIds");
                    Map<String, String> orderIdByMerchant = ctx.require("orderIdByMerchant");
                    saga.markEscrowing(createdOrderIds);
                    List<EscrowAllocationDto> allocations = groups.stream()
                            .map(g -> new EscrowAllocationDto(orderIdByMerchant.get(g.merchantId()),
                                    g.merchantId(), g.subtotal().amount()))
                            .toList();
                    EscrowDto escrow = payment.initEscrow(cmd.idempotencyKey(),
                            cart.grandTotal().amount(), cart.grandTotal().currency().name(),
                            allocations, cmd.buyerId());
                    ctx.put("escrow", escrow);
                })
                .step("complete-session", ctx -> {
                    SubmitCheckoutCmd cmd = ctx.require("cmd");
                    CheckoutSaga saga = ctx.require("saga");
                    CartSnapshot cart = ctx.require("cart");
                    List<String> createdOrderIds = ctx.require("orderIds");
                    EscrowDto escrow = ctx.require("escrow");
                    saga.complete(escrow.paymentUrl());
                    sessions.saveCompleted(cmd.idempotencyKey(), cmd.buyerId(), createdOrderIds,
                            escrow.paymentUrl(), cart.grandTotal().amount());
                })
                .build();
    }

    /**
     * Cancels pending orders newest first, best-effort per order — one failed cancel must not
     * stop the others (nor, when run as the step compensation, the reservation release that
     * follows it). Collected failures surface as one exception: suppressed onto {@code carrier}
     * when undoing a partial creation, or thrown for the workflow to suppress onto the original
     * step failure.
     */
    private void cancelNewestFirst(List<String> createdOrderIds, RuntimeException carrier) {
        List<String> failures = new ArrayList<>();
        for (int i = createdOrderIds.size() - 1; i >= 0; i--) {
            String orderId = createdOrderIds.get(i);
            try {
                order.cancelOrder(orderId, "SAGA_COMPENSATION");
            } catch (RuntimeException e) {
                failures.add("cancelOrder(" + orderId + "): " + e.getMessage());
            }
        }
        if (failures.isEmpty()) {
            return;
        }
        CheckoutDomainException incomplete = new CheckoutDomainException(
                CheckoutErrorCode.CHECKOUT_FAILED, "Order cancellation incomplete: " + failures);
        if (carrier != null) {
            carrier.addSuppressed(incomplete);
        } else {
            throw incomplete;
        }
    }

    private boolean isOutOfStock(RuntimeException failure) {
        return failure instanceof CheckoutDomainException domainFailure
                && domainFailure.checkoutErrorCode() == CheckoutErrorCode.OUT_OF_STOCK;
    }

    private String suppressedMessages(RuntimeException failure) {
        return Arrays.stream(failure.getSuppressed())
                .map(Throwable::getMessage)
                .toList()
                .toString();
    }

    /** Builds the priced cart purely from Catalog data; vetoes unknown/inactive/foreign-merchant SKUs. */
    private CartSnapshot priceCart(SubmitCheckoutCmd cmd) {
        List<String> skus = cmd.items().stream().map(CheckoutItemInput::sku).distinct().toList();
        Map<String, SkuPriceDto> priceBySku = new LinkedHashMap<>();
        for (SkuPriceDto price : catalog.fetchPrices(skus)) {
            priceBySku.put(price.skuCode(), price);
        }

        List<LineItem> lines = new ArrayList<>();
        for (CheckoutItemInput item : cmd.items()) {
            SkuPriceDto price = priceBySku.get(item.sku());
            if (price == null || !price.active()) {
                throw new CheckoutDomainException(CheckoutErrorCode.SKU_UNAVAILABLE,
                        "SKU not available: " + item.sku());
            }
            if (item.merchantId() != null && !item.merchantId().equals(price.merchantId())) {
                throw new CheckoutDomainException(CheckoutErrorCode.SKU_UNAVAILABLE,
                        "SKU " + item.sku() + " does not belong to merchant " + item.merchantId());
            }
            // merchantId from Catalog (authoritative), price from Catalog (authoritative).
            lines.add(new LineItem(item.sku(), item.quantity(), price.merchantId(),
                    Money.of(price.priceAmount(), Currency.of(price.currency()))));
        }
        return new CartSnapshot(lines);
    }

    /** Inventory dedups reserves by (orderRef, sku) — merge duplicate SKU lines into one quantity. */
    private List<ReserveItemDto> aggregateBySku(CartSnapshot cart) {
        Map<String, Integer> qtyBySku = new LinkedHashMap<>();
        for (LineItem line : cart.items()) {
            qtyBySku.merge(line.sku(), line.quantity(), Integer::sum);
        }
        return qtyBySku.entrySet().stream()
                .map(e -> new ReserveItemDto(e.getKey(), e.getValue()))
                .toList();
    }

    private CreateOrderDto toCreateOrder(SubmitCheckoutCmd cmd, MerchantGroup group) {
        SubmitCheckoutCmd.ShippingAddressInput a = cmd.address();
        ShippingAddressDto address = a == null ? null
                : new ShippingAddressDto(a.fullName(), a.phone(), a.addressLine(), a.ward(),
                        a.district(), a.city());
        List<OrderLineDto> lines = group.items().stream()
                // Catalog GetPrice carries no product metadata — the SKU code stands in for
                // productId/variantId/productName until the cart context supplies them.
                .map(li -> new OrderLineDto(li.sku(), li.sku(), li.sku(), li.sku(),
                        li.unitPrice().amount(), li.unitPrice().currency().name(), li.quantity()))
                .toList();
        // One order per merchant → per-order idempotency key (Order has UNIQUE checkout_ref).
        return new CreateOrderDto(cmd.idempotencyKey() + ":" + group.merchantId(), cmd.buyerId(),
                group.merchantId(), address, lines);
    }

    private void validateShape(SubmitCheckoutCmd cmd) {
        if (cmd.items() == null || cmd.items().isEmpty()) {
            throw new CheckoutDomainException(CheckoutErrorCode.EMPTY_CART);
        }
        if (cmd.items().size() > maxItemsPerCart) {
            throw new CheckoutDomainException(CheckoutErrorCode.TOO_MANY_ITEMS,
                    "Cart has " + cmd.items().size() + " items, max " + maxItemsPerCart);
        }
    }

    private CheckoutResultView resultFromCache(CheckoutSession session) {
        if (session.isRedirected()) {
            return new CheckoutResultView(session.paymentUrl(), session.orderIds(), session.grandTotal());
        }
        throw new CheckoutDomainException(CheckoutErrorCode.CHECKOUT_FAILED,
                "Previous attempt with this key failed (" + session.failReason()
                        + ") — retry with a new idempotency key");
    }
}
