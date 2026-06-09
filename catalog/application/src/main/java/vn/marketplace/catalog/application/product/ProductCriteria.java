package vn.marketplace.catalog.application.product;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Criteria;

/**
 * Query criteria for the product repository port. Nullable fields = "no filter on this dimension";
 * the outbound adapter interprets them (DB query in production; ES for full-text search).
 */
public record ProductCriteria(List<String> skuCodes,
                              String merchantId,
                              Boolean activeOnly,
                              String text,
                              String categoryId,
                              String brandId,
                              Long priceMin,
                              Long priceMax) implements Criteria {

    public static ProductCriteria bySkuCodes(List<String> skuCodes) {
        return new ProductCriteria(skuCodes, null, null, null, null, null, null, null);
    }

    public static ProductCriteria byMerchant(String merchantId) {
        return new ProductCriteria(null, merchantId, null, null, null, null, null, null);
    }

    public static ProductCriteria activeSearch(String text, String categoryId, String brandId,
                                               Long priceMin, Long priceMax) {
        return new ProductCriteria(null, null, true, text, categoryId, brandId, priceMin, priceMax);
    }
}
