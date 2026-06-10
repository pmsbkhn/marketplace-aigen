package vn.marketplace.notification.adapter.delivery.dto;

/** Inbound {@code OrderCompleted} payload (stand-in for the Kafka event from Order). */
public record OrderCompletedEvent(String eventId, String orderId, String buyerId, String merchantId) {
}
