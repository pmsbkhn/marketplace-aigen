package vn.marketplace.inventory.adapter.stock.dto;

import java.util.List;

/** Body for the internal reserve endpoint (stand-in for gRPC {@code Inventory.ReserveStock}). */
public record ReserveStockRequest(String orderRef, List<Item> items) {

    public record Item(String sku, int quantity) {
    }
}
