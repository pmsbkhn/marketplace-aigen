package vn.marketplace.catalog.application.product;

import java.util.List;
import java.util.Map;

/** Shared builders for application-layer tests. */
final class CatalogTestData {

    private CatalogTestData() {
    }

    static CreateProductCmd createCmd(String merchantId, String skuCode, long price) {
        return new CreateProductCmd(
                "T-Shirt",
                merchantId,
                merchantId,
                "B-1",
                "C-1",
                "A plain t-shirt",
                List.of(new CreateProductCmd.VariantInput(
                        "red-M",
                        Map.of("color", "red"),
                        List.of(new CreateProductCmd.SkuInput(skuCode, price, "VND")))),
                List.of());
    }
}
