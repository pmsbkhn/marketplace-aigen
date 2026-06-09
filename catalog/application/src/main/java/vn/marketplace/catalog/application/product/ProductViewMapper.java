package vn.marketplace.catalog.application.product;

import java.util.List;

import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.Sku;
import vn.marketplace.catalog.domain.product.management.Variant;

/** Pure domain → view mapping. No framework dependencies. */
final class ProductViewMapper {

    private ProductViewMapper() {
    }

    static ProductView toDetail(Product product) {
        List<ProductView.VariantView> variants = product.variants().stream()
                .map(ProductViewMapper::toVariantView)
                .toList();
        List<String> imageUrls = product.images().stream()
                .map(im -> im.url())
                .toList();
        return new ProductView(
                product.id().value(),
                product.merchantId().value(),
                product.name(),
                product.description(),
                product.status().name(),
                product.brandId() == null ? null : product.brandId().value(),
                product.categoryId() == null ? null : product.categoryId().value(),
                variants,
                imageUrls,
                product.moderatedBy(),
                product.moderationReason(),
                product.createdAt(),
                product.updatedAt());
    }

    static ProductSummaryView toSummary(Product product) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Variant v : product.variants()) {
            for (Sku s : v.skus()) {
                long amt = s.price().amount();
                min = Math.min(min, amt);
                max = Math.max(max, amt);
            }
        }
        if (min == Long.MAX_VALUE) {
            min = 0;
            max = 0;
        }
        String imageUrl = product.images().isEmpty() ? null : product.images().get(0).url();
        return new ProductSummaryView(
                product.id().value(),
                product.name(),
                product.merchantId().value(),
                product.status().name(),
                min,
                max,
                product.brandId() == null ? null : product.brandId().value(),
                product.categoryId() == null ? null : product.categoryId().value(),
                imageUrl);
    }

    private static ProductView.VariantView toVariantView(Variant v) {
        List<ProductView.SkuView> skus = v.skus().stream()
                .map(s -> new ProductView.SkuView(s.code().value(), s.price().amount(), s.price().currency().name()))
                .toList();
        return new ProductView.VariantView(v.id().value(), v.name(), v.attributes(), skus);
    }
}
