package vn.marketplace.checkout.application.checkout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import vn.marketplace.checkout.domain.checkout.IdempotencyKey;
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
 * No {@code @EventPublishHandler}: Checkout persists nothing through an msfw Repository and
 * publishes no domain events — its durable effects live in the downstream contexts.
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

    public SubmitCheckoutUc(CatalogPort catalog, InventoryPort inventory, OrderPort order,
                            PaymentPort payment, CheckoutSessionPort sessions,
                            int maxItemsPerCart, int maxMerchantsPerCart) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.order = order;
        this.payment = payment;
        this.sessions = sessions;
        this.maxItemsPerCart = maxItemsPerCart;
        this.maxMerchantsPerCart = maxMerchantsPerCart;
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

        // ---- PRICING: Catalog is the only price authority (TC-CHK-03) ----
        CartSnapshot cart = priceCart(cmd);
        List<MerchantGroup> groups = splitter.split(cart);
        if (groups.size() > maxMerchantsPerCart) {
            throw new CheckoutDomainException(CheckoutErrorCode.TOO_MANY_MERCHANTS);
        }

        // ---- RESERVING: all-or-nothing; a shortage holds nothing → fail fast, no compensation ----
        saga.markReserving();
        ReservationDto reservation = inventory.reserveStock(cmd.idempotencyKey(), aggregateBySku(cart));
        if (!reservation.allReserved()) {
            saga.fail("OUT_OF_STOCK");
            throw new CheckoutDomainException(CheckoutErrorCode.OUT_OF_STOCK,
                    "Out of stock: " + reservation.shortages());
        }

        // ---- ORDERING + ESCROWING: side effects exist now → compensate in reverse on failure ----
        List<String> createdOrderIds = new ArrayList<>();
        try {
            saga.markOrdering();
            Map<String, String> orderIdByMerchant = new LinkedHashMap<>();
            for (MerchantGroup group : groups) {
                String orderId = order.createPendingOrder(toCreateOrder(cmd, group));
                createdOrderIds.add(orderId);
                orderIdByMerchant.put(group.merchantId(), orderId);
            }

            saga.markEscrowing(createdOrderIds);
            List<EscrowAllocationDto> allocations = groups.stream()
                    .map(g -> new EscrowAllocationDto(orderIdByMerchant.get(g.merchantId()),
                            g.merchantId(), g.subtotal().amount()))
                    .toList();
            long grandTotal = cart.grandTotal().amount();
            EscrowDto escrow = payment.initEscrow(cmd.idempotencyKey(), grandTotal,
                    cart.grandTotal().currency().name(), allocations, cmd.buyerId());

            saga.complete(escrow.paymentUrl());
            sessions.saveCompleted(cmd.idempotencyKey(), cmd.buyerId(), createdOrderIds,
                    escrow.paymentUrl(), grandTotal);
            return new CheckoutResultView(escrow.paymentUrl(), createdOrderIds, grandTotal);

        } catch (RuntimeException stepFailure) {
            compensate(createdOrderIds, reservation.reservationId());
            saga.fail(stepFailure.getMessage());
            sessions.saveFailed(cmd.idempotencyKey(), cmd.buyerId(), stepFailure.getMessage());
            throw new CheckoutDomainException(CheckoutErrorCode.CHECKOUT_FAILED,
                    "Checkout failed and was compensated — retry with a new idempotency key. Cause: "
                            + stepFailure.getMessage());
        }
    }

    /**
     * Strict reverse-order compensation (TC-CHK-02): cancel the pending orders FIRST (newest
     * first), release the stock reservation LAST. Best-effort per step — one failed cancel must
     * not leave the reservation held forever.
     */
    private void compensate(List<String> createdOrderIds, String reservationId) {
        List<String> compensationFailures = new ArrayList<>();
        for (int i = createdOrderIds.size() - 1; i >= 0; i--) {
            String orderId = createdOrderIds.get(i);
            try {
                order.cancelOrder(orderId, "SAGA_COMPENSATION");
            } catch (RuntimeException e) {
                compensationFailures.add("cancelOrder(" + orderId + "): " + e.getMessage());
            }
        }
        try {
            inventory.releaseStock(reservationId);
        } catch (RuntimeException e) {
            compensationFailures.add("releaseStock(" + reservationId + "): " + e.getMessage());
        }
        if (!compensationFailures.isEmpty()) {
            // Surface loudly: a ghost reservation/order needs the reconciliation runbook.
            throw new CheckoutDomainException(CheckoutErrorCode.CHECKOUT_FAILED,
                    "COMPENSATION INCOMPLETE — manual reconciliation required: " + compensationFailures);
        }
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
