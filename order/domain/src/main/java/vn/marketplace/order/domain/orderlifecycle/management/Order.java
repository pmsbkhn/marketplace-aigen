package vn.marketplace.order.domain.orderlifecycle.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import vn.marketplace.order.domain.orderlifecycle.BuyerId;
import vn.marketplace.order.domain.orderlifecycle.Currency;
import vn.marketplace.order.domain.orderlifecycle.HistoryId;
import vn.marketplace.order.domain.orderlifecycle.MerchantId;
import vn.marketplace.order.domain.orderlifecycle.Money;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.OrderItemId;
import vn.marketplace.order.domain.orderlifecycle.OrderStatus;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.shared.InvalidTransitionException;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

/**
 * Order aggregate root — the order-lifecycle source of truth.
 *
 * <p>State machine (the only legal transitions; terminal states immutable):
 * <pre>
 *   PENDING --receivePayment--> TO_SHIP --ship--> SHIPPED --complete--> COMPLETED
 *   PENDING | TO_SHIP --cancel--> CANCELLED
 * </pre>
 * Any other transition throws {@link InvalidTransitionException} (e.g. PENDING→SHIPPED, or cancelling a
 * SHIPPED/COMPLETED order). Enforced invariants: non-empty order (EMPTY_CART), {@code qty>0} &amp;
 * {@code price>0} per item, {@code totalAmount == Σ priceSnapshot×qty}, immutable price snapshots,
 * append-only status history, immutable {@code buyerId}/{@code merchantId}/{@code checkoutRef}.
 * {@code OrderCompleted}/{@code OrderCancelled} are published exactly on the matching transition.
 */
public class Order extends Aggregate<OrderId> {

    private final String checkoutRef;
    private final BuyerId buyerId;
    private final MerchantId merchantId;
    private OrderStatus status;
    private final Money totalAmount;
    private final ShippingAddress shippingAddress;
    private final List<OrderItem> items;
    private final List<OrderStatusHistory> statusHistory;
    private String trackingNumber;
    private String cancelReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order(OrderId id, String checkoutRef, BuyerId buyerId, MerchantId merchantId,
                  ShippingAddress shippingAddress, List<OrderItem> items, String triggeredBy) {
        if (items == null || items.isEmpty()) {
            throw new OrderDomainException(OrderErrorCode.EMPTY_CART);
        }
        this.id = id;
        this.checkoutRef = checkoutRef;
        this.buyerId = buyerId;
        this.merchantId = merchantId;
        this.shippingAddress = shippingAddress;
        this.items = new ArrayList<>(items);
        this.totalAmount = computeTotal(this.items);
        this.status = OrderStatus.PENDING;
        this.statusHistory = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        appendHistory(null, OrderStatus.PENDING, triggeredBy, null);
    }

    /** Reconstruction constructor — restores full state, raises no events. */
    private Order(Memento m, List<OrderItem> items, List<OrderStatusHistory> history) {
        this.id = new OrderId(m.orderId());
        this.set_id(m._id());
        this.checkoutRef = m.checkoutRef();
        this.buyerId = new BuyerId(m.buyerId());
        this.merchantId = new MerchantId(m.merchantId());
        this.status = OrderStatus.valueOf(m.status());
        this.totalAmount = Money.of(m.totalAmount(), Currency.of(m.currency()));
        this.shippingAddress = m.shippingAddress();
        this.items = new ArrayList<>(items);
        this.statusHistory = new ArrayList<>(history);
        this.trackingNumber = m.trackingNumber();
        this.cancelReason = m.cancelReason();
        this.createdAt = m.createdAt();
        this.updatedAt = m.updatedAt();
    }

    /** Create a new PENDING order (called by Checkout via CreatePendingOrder). */
    public static Order createPending(OrderId id, String checkoutRef, BuyerId buyerId, MerchantId merchantId,
                                      ShippingAddress shippingAddress, List<NewItem> newItems, String triggeredBy) {
        List<OrderItem> items = new ArrayList<>();
        if (newItems != null) {
            for (NewItem ni : newItems) {
                items.add(new OrderItem(new OrderItemId(ni.orderItemId()), ni.productId(), ni.variantId(),
                        ni.skuCode(), ni.productName(), Money.of(ni.priceAmount(), Currency.of(ni.currency())), ni.qty()));
            }
        }
        return new Order(id, checkoutRef, buyerId, merchantId, shippingAddress, items, triggeredBy);
    }

    // ---- verb methods (state machine — the only way to change status) ----

    /** PENDING → TO_SHIP (payment received). */
    public void receivePayment(String triggeredBy) {
        requireStatus(OrderStatus.PENDING, "PAYMENT_RECEIVED");
        transitionTo(OrderStatus.TO_SHIP, triggeredBy, null);
    }

    /** TO_SHIP → SHIPPED (merchant ships; tracking number required). */
    public void ship(String trackingNumber, String triggeredBy) {
        requireStatus(OrderStatus.TO_SHIP, "MERCHANT_SHIP");
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new OrderDomainException(OrderErrorCode.TRACKING_REQUIRED);
        }
        this.trackingNumber = trackingNumber;
        transitionTo(OrderStatus.SHIPPED, triggeredBy, null);
    }

    /** SHIPPED → COMPLETED (buyer confirms / auto-complete). Publishes {@code OrderCompleted}. */
    public void complete(String triggeredBy) {
        requireStatus(OrderStatus.SHIPPED, "BUYER_CONFIRM");
        transitionTo(OrderStatus.COMPLETED, triggeredBy, null);
        DomainEventPublisher.publish(new OrderCompleted(this, DTime.now()));
    }

    /** PENDING | TO_SHIP → CANCELLED. No-op if already CANCELLED; illegal for SHIPPED/COMPLETED. Publishes {@code OrderCancelled}. */
    public void cancel(String reason, String triggeredBy) {
        if (this.status == OrderStatus.CANCELLED) {
            return; // idempotent
        }
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.TO_SHIP) {
            throw new InvalidTransitionException(this.status, "CANCEL");
        }
        this.cancelReason = reason;
        transitionTo(OrderStatus.CANCELLED, triggeredBy, reason);
        DomainEventPublisher.publish(new OrderCancelled(this, reason, DTime.now()));
    }

    private void requireStatus(OrderStatus expected, String event) {
        if (this.status != expected) {
            throw new InvalidTransitionException(this.status, event);
        }
    }

    private void transitionTo(OrderStatus next, String triggeredBy, String reason) {
        OrderStatus from = this.status;
        this.status = next;
        this.updatedAt = LocalDateTime.now();
        appendHistory(from, next, triggeredBy, reason);
    }

    private void appendHistory(OrderStatus from, OrderStatus to, String triggeredBy, String reason) {
        statusHistory.add(new OrderStatusHistory(new HistoryId(UUID.randomUUID().toString()),
                from, to, triggeredBy, reason, LocalDateTime.now()));
    }

    private static Money computeTotal(List<OrderItem> items) {
        Money total = Money.of(0, items.get(0).subtotal().currency());
        for (OrderItem item : items) {
            total = total.plus(item.subtotal());
        }
        return total;
    }

    // ---- queries / accessors ----

    public String checkoutRef() {
        return checkoutRef;
    }

    public BuyerId buyerId() {
        return buyerId;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public OrderStatus status() {
        return status;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public ShippingAddress shippingAddress() {
        return shippingAddress;
    }

    public List<OrderItem> items() {
        return List.copyOf(items);
    }

    public List<OrderStatusHistory> statusHistory() {
        return List.copyOf(statusHistory);
    }

    public String trackingNumber() {
        return trackingNumber;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /** Input spec for one new line at order creation. */
    public record NewItem(String orderItemId, String productId, String variantId, String skuCode,
                          String productName, long priceAmount, String currency, int qty) {
    }

    // ---- Memento (persistence reconstruction) ----

    public static Order fromMemento(Memento m) {
        List<OrderItem> items = m.items().stream().map(OrderItem::fromMemento).toList();
        List<OrderStatusHistory> history = m.statusHistory().stream().map(OrderStatusHistory::fromMemento).toList();
        return new Order(m, items, history);
    }

    public Memento toMemento() {
        List<OrderItemMemento> itemMementos = new ArrayList<>();
        for (OrderItem i : items) {
            itemMementos.add(new OrderItemMemento(i.id().value(), i.productId(), i.variantId(), i.skuCode(),
                    i.productName(), i.priceSnapshot().amount(), i.priceSnapshot().currency().name(), i.qty()));
        }
        List<StatusHistoryMemento> historyMementos = new ArrayList<>();
        for (OrderStatusHistory h : statusHistory) {
            historyMementos.add(new StatusHistoryMemento(h.id().value(),
                    h.fromStatus() == null ? null : h.fromStatus().name(), h.toStatus().name(),
                    h.triggeredBy(), h.reason(), h.timestamp()));
        }
        return new Memento(_id(), id.value(), checkoutRef, buyerId.value(), merchantId.value(), status.name(),
                totalAmount.amount(), totalAmount.currency().name(), shippingAddress, trackingNumber, cancelReason,
                createdAt, updatedAt, itemMementos, historyMementos);
    }

    public record Memento(Long _id,
                          String orderId,
                          String checkoutRef,
                          String buyerId,
                          String merchantId,
                          String status,
                          long totalAmount,
                          String currency,
                          ShippingAddress shippingAddress,
                          String trackingNumber,
                          String cancelReason,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          List<OrderItemMemento> items,
                          List<StatusHistoryMemento> statusHistory) {
    }

    public record OrderItemMemento(String orderItemId, String productId, String variantId, String skuCode,
                                   String productName, long priceAmount, String currency, int qty) {
    }

    public record StatusHistoryMemento(String historyId, String fromStatus, String toStatus,
                                       String triggeredBy, String reason, LocalDateTime timestamp) {
    }
}
