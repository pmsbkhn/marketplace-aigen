package vn.marketplace.catalog.application.product;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.marketplace.catalog.domain.product.BrandId;
import vn.marketplace.catalog.domain.product.CategoryId;
import vn.marketplace.catalog.domain.product.Currency;
import vn.marketplace.catalog.domain.product.MerchantId;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.ProductImage;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.SkuId;
import vn.marketplace.catalog.domain.product.VariantId;
import vn.marketplace.catalog.domain.product.management.ProductFactoryParams;

/**
 * Command to create a product. {@code merchantId}/{@code createdBy} come from the caller's JWT at the
 * adapter (never from the client body — anti-spoofing). Identities for the new product/variants/skus
 * are generated here.
 */
public record CreateProductCmd(String name,
                               String merchantId,
                               String createdBy,
                               String brandId,
                               String categoryId,
                               String description,
                               List<VariantInput> variants,
                               List<ImageInput> images) {

    public record VariantInput(String name, Map<String, String> attributes, List<SkuInput> skus) {
    }

    public record SkuInput(String skuCode, long priceAmount, String currency) {
    }

    public record ImageInput(String url, int sortOrder) {
    }

    ProductFactoryParams produceFactoryParams() {
        List<ProductFactoryParams.VariantSpec> variantSpecs = variants == null ? List.of()
                : variants.stream().map(this::toVariantSpec).toList();
        List<ProductImage> imageList = images == null ? List.of()
                : images.stream().map(i -> new ProductImage(i.url(), i.sortOrder())).toList();

        return new ProductFactoryParams(
                new ProductId(UUID.randomUUID().toString()),
                new MerchantId(merchantId),
                name,
                description,
                brandId == null ? null : new BrandId(brandId),
                categoryId == null ? null : new CategoryId(categoryId),
                createdBy,
                variantSpecs,
                imageList);
    }

    private ProductFactoryParams.VariantSpec toVariantSpec(VariantInput v) {
        List<ProductFactoryParams.SkuSpec> skuSpecs = (v.skus() == null ? List.<SkuInput>of() : v.skus())
                .stream()
                .map(s -> new ProductFactoryParams.SkuSpec(
                        new SkuId(UUID.randomUUID().toString()),
                        new SkuCode(s.skuCode()),
                        s.priceAmount(),
                        Currency.of(s.currency() == null ? "VND" : s.currency())))
                .toList();
        return new ProductFactoryParams.VariantSpec(
                new VariantId(UUID.randomUUID().toString()),
                v.name(),
                v.attributes() == null ? Map.of() : v.attributes(),
                skuSpecs);
    }
}
