package vn.marketplace.order.application.order;

/**
 * Forward-transition command. {@code trackingNumber} is required for {@code MERCHANT_SHIP};
 * {@code eventId} supports consumer idempotency for the event-driven transition ({@code PAYMENT_RECEIVED}).
 */
public record TransitionOrderCmd(String orderId, TransitionEvent event, String triggeredBy,
                                 String trackingNumber, String eventId) {
}
