package vn.marketplace.inventory.application.stock;

/** Read model returned after a merchant stock update. */
public record StockView(String sku, int available, int reserved, String merchantId, long version) {
}
