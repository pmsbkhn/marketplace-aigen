package vn.marketplace.catalog.domain.product.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Factory;
import vn.marketplace.catalog.domain.product.ProductImage;

/**
 * Builds a new {@link Product} (always PENDING) from {@link ProductFactoryParams}.
 *
 * <p>Creation publishes NO event — per the spec, {@code ProductCreated} is raised exactly once when
 * the product first becomes ACTIVE (see {@link Product#approve}). The factory only assembles the
 * Variant/SKU graph, surfacing structural invariants (≥1 variant, ≥1 SKU, price &gt; 0) early.
 */
public class ProductFactory implements Factory<Product, ProductFactoryParams> {

    @Override
    public Product create(ProductFactoryParams params) {
        List<Variant> variants = params.variants().stream()
                .map(this::toVariant)
                .toList();
        List<ProductImage> images = params.images() == null ? List.of() : params.images();

        return new Product(params.id(),
                params.merchantId(),
                params.name(),
                params.description(),
                params.brandId(),
                params.categoryId(),
                variants,
                images,
                params.createdBy());
    }

    private Variant toVariant(ProductFactoryParams.VariantSpec spec) {
        List<Sku> skus = spec.skus().stream()
                .map(s -> new Sku(s.id(), s.code(), s.priceAmount(), s.currency()))
                .toList();
        return new Variant(spec.id(), spec.name(), spec.attributes(), skus);
    }
}
