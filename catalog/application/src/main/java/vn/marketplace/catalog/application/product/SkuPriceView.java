package vn.marketplace.catalog.application.product;

/**
 * Price snapshot for one SKU, served to Checkout (gRPC GetPrice in production). {@code active} is
 * false when the SKU is missing or its product is not ACTIVE — Checkout decides the user-facing error.
 */
public record SkuPriceView(String skuCode, long priceAmount, String currency, String merchantId, boolean active) {
}
