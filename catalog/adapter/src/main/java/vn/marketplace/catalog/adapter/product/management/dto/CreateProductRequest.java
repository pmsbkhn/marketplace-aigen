package vn.marketplace.catalog.adapter.product.management.dto;

import java.util.List;
import java.util.Map;

import vn.marketplace.catalog.application.product.CreateProductCmd;

/**
 * Create-product request body. {@code merchantId}/{@code createdBy} are NOT in the body — they come
 * from the caller's identity at the adapter (anti-spoofing).
 */
public record CreateProductRequest(String name,
                                   String brandId,
                                   String categoryId,
                                   String description,
                                   List<VariantRequest> variants,
                                   List<ImageRequest> images) {

    public record VariantRequest(String name, Map<String, String> attributes, List<SkuRequest> skus) {
    }

    public record SkuRequest(String skuCode, long priceAmount, String currency) {
    }

    public record ImageRequest(String url, int sortOrder) {
    }

    public CreateProductCmd toCmd(String merchantId, String createdBy) {
        List<CreateProductCmd.VariantInput> variantInputs = (variants == null ? List.<VariantRequest>of() : variants)
                .stream()
                .map(v -> new CreateProductCmd.VariantInput(
                        v.name(),
                        v.attributes(),
                        (v.skus() == null ? List.<SkuRequest>of() : v.skus()).stream()
                                .map(s -> new CreateProductCmd.SkuInput(s.skuCode(), s.priceAmount(), s.currency()))
                                .toList()))
                .toList();
        List<CreateProductCmd.ImageInput> imageInputs = (images == null ? List.<ImageRequest>of() : images)
                .stream()
                .map(i -> new CreateProductCmd.ImageInput(i.url(), i.sortOrder()))
                .toList();
        return new CreateProductCmd(name, merchantId, createdBy, brandId, categoryId, description,
                variantInputs, imageInputs);
    }
}
