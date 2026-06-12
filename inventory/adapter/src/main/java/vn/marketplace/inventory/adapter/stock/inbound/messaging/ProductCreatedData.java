package vn.marketplace.inventory.adapter.stock.inbound.messaging;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire shape of Catalog's {@code ProductCreated} event payload (the JSON serialization of the
 * domain event). Only the fields this service consumes are declared; the rest is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCreatedData(String productId, String merchantId, List<String> skuCodes) {
}
