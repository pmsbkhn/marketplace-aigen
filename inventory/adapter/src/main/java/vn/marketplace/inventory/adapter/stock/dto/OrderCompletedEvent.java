package vn.marketplace.inventory.adapter.stock.dto;

import java.util.List;

/**
 * Inbound {@code OrderCompleted} payload (stand-in for the Kafka event from Order). {@code orderId} is
 * the reservation handle; {@code skus} are the SKUs to consume. Drives {@code DeductStock}.
 */
public record OrderCompletedEvent(String eventId, String orderId, List<String> skus) {
}
