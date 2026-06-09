package vn.marketplace.inventory.adapter.stock.dto;

import java.util.List;

/** Body for the internal get-stock-level endpoint (stand-in for gRPC {@code Inventory.GetStockLevel}). */
public record StockLevelsRequest(List<String> skuCodes) {
}
