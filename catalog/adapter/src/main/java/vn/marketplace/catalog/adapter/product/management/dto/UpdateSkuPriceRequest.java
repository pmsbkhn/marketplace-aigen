package vn.marketplace.catalog.adapter.product.management.dto;

/** Body for PUT /v1/products/{id}/skus/{skuCode}: the new SKU price (minor units, must be &gt; 0). */
public record UpdateSkuPriceRequest(long priceAmount) {
}
