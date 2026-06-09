package vn.marketplace.inventory.application.stock;

/** Read model for one SKU's current stock level. */
public record StockLevelView(String sku, int available, int reserved) {
}
