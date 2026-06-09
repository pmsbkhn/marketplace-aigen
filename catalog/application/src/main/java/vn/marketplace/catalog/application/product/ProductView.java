package vn.marketplace.catalog.application.product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Full product detail view (GET /v1/products/{id}). */
public record ProductView(String id,
                          String merchantId,
                          String name,
                          String description,
                          String status,
                          String brandId,
                          String categoryId,
                          List<VariantView> variants,
                          List<String> imageUrls,
                          String moderatedBy,
                          String moderationReason,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {

    public record VariantView(String id, String name, Map<String, String> attributes, List<SkuView> skus) {
    }

    public record SkuView(String skuCode, long priceAmount, String currency) {
    }
}
