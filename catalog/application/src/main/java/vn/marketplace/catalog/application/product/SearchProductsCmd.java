package vn.marketplace.catalog.application.product;

/** Storefront search/filter request. Only ACTIVE products are ever returned. */
public record SearchProductsCmd(String query,
                                String categoryId,
                                String brandId,
                                Long priceMin,
                                Long priceMax,
                                int page,
                                int size) {
}
