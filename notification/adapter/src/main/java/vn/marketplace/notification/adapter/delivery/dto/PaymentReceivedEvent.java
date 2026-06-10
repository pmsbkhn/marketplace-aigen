package vn.marketplace.notification.adapter.delivery.dto;

/** Inbound {@code PaymentReceived} payload (stand-in for the Kafka event from Payment). */
public record PaymentReceivedEvent(String eventId, String orderId, String buyerId, long amount) {
}
