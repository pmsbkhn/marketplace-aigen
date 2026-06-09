package vn.marketplace.catalog.application.product;

/** Use-case port: storefront full-text search/filter (ACTIVE only). */
public interface SearchProducts {
    ProductSearchResult execute(SearchProductsCmd cmd);
}
