package vn.marketplace.inventory.application.stock;

import java.util.List;

/**
 * Init-stock command (from {@code ProductCreated}). {@code eventId} is for consumer idempotency at the
 * adapter; {@code skuCodes} are the SKUs to create at quantity 0, all owned by {@code merchantId}.
 */
public record InitSkuCmd(String eventId, String productId, List<String> skuCodes, String merchantId) {
}
