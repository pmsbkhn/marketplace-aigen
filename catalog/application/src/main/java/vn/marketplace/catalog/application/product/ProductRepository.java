package vn.marketplace.catalog.application.product;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.catalog.domain.product.management.Product;

/**
 * Catalog-specific repository port. Extends the generic msfw {@link Repository} with the two lookups
 * the Criteria DSL cannot express (they require joining the variant/SKU collections); simple
 * root-attribute filters go through {@code findBy(Criteria)} with the DSL instead.
 */
public interface ProductRepository extends Repository<Product> {

    /** Products owning any of the given SKU codes — DB source of truth for checkout pricing. */
    List<Product> findBySkuCodes(List<String> skuCodes);

    /**
     * Paged storefront search over ACTIVE products (DB stand-in for Elasticsearch). Nullable
     * parameters mean "no filter on this dimension"; a product matches a price bound when ANY of
     * its SKUs falls inside the range.
     */
    PagedSearchResult<Product> searchActive(String text,
                                            String categoryId,
                                            String brandId,
                                            Long priceMin,
                                            Long priceMax,
                                            Pagination pagination);
}
