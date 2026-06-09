package vn.marketplace.catalog.application.product;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.catalog.domain.product.management.Product;

/**
 * Storefront search. Read-only. Pagination is 0-based ({@code page=0} is the first page); negative
 * pages clamp to 0 and a non-positive size falls back to a sane default.
 *
 * <p>In production this fans out to Elasticsearch; here the repository port abstracts it (the DB-backed
 * adapter applies the same ACTIVE-only filter).
 */
public class SearchProductsUc implements SearchProducts {

    private static final int DEFAULT_PAGE_SIZE = 24;
    private static final int MAX_PAGE_SIZE = 100;

    private final Repository<Product> productRepository;

    public SearchProductsUc(Repository<Product> productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductSearchResult execute(SearchProductsCmd cmd) {
        int page = Math.max(0, cmd.page());
        int size = cmd.size() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(cmd.size(), MAX_PAGE_SIZE);

        ProductCriteria criteria = ProductCriteria.activeSearch(
                cmd.query(), cmd.categoryId(), cmd.brandId(), cmd.priceMin(), cmd.priceMax());

        PagedSearchResult<Product> result = productRepository.findBy(criteria, Pagination.of(page, size));
        List<ProductSummaryView> summaries = result.content().stream()
                .map(ProductViewMapper::toSummary)
                .toList();

        return new ProductSearchResult(summaries, summaries.size(), result.page(), result.size());
    }
}
