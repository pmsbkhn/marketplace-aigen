package vn.marketplace.order.adapter.order.dto;

/** Inbound {@code PaymentReceived} payload (stand-in for the Kafka event from Payment). */
public record PaymentReceivedEvent(String eventId, String orderId, String txnId, long amount) {
}
