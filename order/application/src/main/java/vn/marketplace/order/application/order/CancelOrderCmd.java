package vn.marketplace.order.application.order;

/** Cancel command. {@code reason} e.g. SAGA_COMPENSATION / PAYMENT_TIMEOUT / a buyer/admin reason. */
public record CancelOrderCmd(String orderId, String triggeredBy, String reason) {
}
