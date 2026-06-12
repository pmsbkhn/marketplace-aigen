package vn.marketplace.inventory.adapter.stock.inbound.messaging;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire shape of Order's {@code OrderCompleted} event payload (the JSON serialization of the
 * domain event). Only the fields this service consumes are declared; the rest is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCompletedData(String orderId, String merchantId, List<Line> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(String sku, int qty) {
    }
}
