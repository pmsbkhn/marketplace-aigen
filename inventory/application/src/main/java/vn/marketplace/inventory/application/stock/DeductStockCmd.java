package vn.marketplace.inventory.application.stock;

import java.util.List;

/**
 * Deduct command (from {@code OrderCompleted}). {@code eventId} is for consumer idempotency at the
 * adapter; {@code orderId} is the order's reservation handle; {@code skus} are the SKUs to consume.
 */
public record DeductStockCmd(String eventId, String orderId, List<String> skus) {
}
