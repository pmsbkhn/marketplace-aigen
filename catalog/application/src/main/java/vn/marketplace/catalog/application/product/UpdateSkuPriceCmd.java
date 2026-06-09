package vn.marketplace.catalog.application.product;

import vn.marketplace.catalog.domain.shared.Actor;

/**
 * Command to change one SKU's price (PUT /v1/products/{id}/skus/{skuCode}). {@code caller} is built
 * from the verified JWT at the adapter; the use-case enforces that only the owning Merchant may change
 * the price (tenant scope). Price validation (&gt; 0) lives in the domain.
 */
public record UpdateSkuPriceCmd(String productId, String skuCode, long newAmount, Actor caller) {
}
