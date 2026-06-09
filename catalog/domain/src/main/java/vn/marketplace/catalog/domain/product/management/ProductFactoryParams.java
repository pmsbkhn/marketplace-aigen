package vn.marketplace.catalog.domain.product.management;

import java.util.List;
import java.util.Map;

import tech.vsf.ptnt.msfw.domain.core.FactoryParams;
import vn.marketplace.catalog.domain.product.BrandId;
import vn.marketplace.catalog.domain.product.CategoryId;
import vn.marketplace.catalog.domain.product.Currency;
import vn.marketplace.catalog.domain.product.MerchantId;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.ProductImage;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.SkuId;
import vn.marketplace.catalog.domain.product.VariantId;

/**
 * Raw inputs for {@link ProductFactory}. Carries plain specs (not built entities) because Variant/Sku
 * constructors are package-private — the factory assembles the aggregate graph.
 */
public record ProductFactoryParams(ProductId id,
                                   MerchantId merchantId,
                                   String name,
                                   String description,
                                   BrandId brandId,
                                   CategoryId categoryId,
                                   String createdBy,
                                   List<VariantSpec> variants,
                                   List<ProductImage> images) implements FactoryParams {

    public record VariantSpec(VariantId id, String name, Map<String, String> attributes, List<SkuSpec> skus) {
    }

    public record SkuSpec(SkuId id, SkuCode code, long priceAmount, Currency currency) {
    }
}
