package vn.marketplace.order.adapter.order.dto;

/** Inbound {@code PaymentFailed} payload (stand-in for the Kafka event from Payment). */
public record PaymentFailedEvent(String eventId, String orderId, String reason) {
}
