package vn.marketplace.inventory.application.stock;

/**
 * Merchant stock-update command (PUT /v1/merchant/stock/{sku}). {@code callerMerchantId} comes from the
 * verified JWT; the domain enforces that it owns the SKU (tenant scope).
 */
public record UpdateMerchantStockCmd(String sku, int available, String callerMerchantId) {
}
