package vn.marketplace.catalog.application.product;

import java.util.List;

/** Paginated search/listing result. */
public record ProductSearchResult(List<ProductSummaryView> results, long total, int page, int size) {
}
