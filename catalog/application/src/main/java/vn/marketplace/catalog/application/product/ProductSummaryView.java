package vn.marketplace.catalog.application.product;

/** Compact product entry for search/listing results. */
public record ProductSummaryView(String productId,
                                 String name,
                                 String merchantId,
                                 String status,
                                 long priceMin,
                                 long priceMax,
                                 String brandId,
                                 String categoryId,
                                 String imageUrl) {
}
