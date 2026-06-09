package vn.marketplace.inventory.adapter.stock.dto;

import java.util.List;

/**
 * Inbound {@code ProductCreated} payload (stand-in for the Kafka event from Catalog). Matches Catalog's
 * {@code ProductCreated}: productId + merchantId + skuCodes. Drives {@code InitSku}.
 */
public record ProductCreatedEvent(String eventId, String productId, String merchantId, List<String> skuCodes) {
}
