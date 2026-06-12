package vn.marketplace.order.application.order;

/** Expiry command, carried by the {@code Order/OrderPendingTimedOut} delayed event. */
public record ExpirePendingOrderCmd(String orderId) {
}
