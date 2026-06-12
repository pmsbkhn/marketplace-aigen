package vn.marketplace.catalog.domain.product.management;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.type.DTime;
import tech.vsf.ptnt.msfw.test.JsonEventContract;
import vn.marketplace.catalog.domain.product.BrandId;
import vn.marketplace.catalog.domain.product.CategoryId;
import vn.marketplace.catalog.domain.product.Currency;
import vn.marketplace.catalog.domain.product.MerchantId;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.SkuId;
import vn.marketplace.catalog.domain.product.VariantId;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of the {@code Catalog/ProductCreated} JSON contract: writes the exact wire
 * envelope of a representative event into {@code <repo>/contracts}. The file is committed —
 * a git diff on it IS the contract-change signal; consumers (inventory) bind their DTOs from it.
 */
class ProductCreatedContractTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");
    private static final DTime FIXED_TIME = DTime.of(LocalDateTime.of(2026, 6, 12, 12, 0));

    @Test
    void publishesTheProductCreatedContract() throws Exception {
        Product product = new ProductFactory().create(new ProductFactoryParams(
                new ProductId("P-1"),
                new MerchantId("M-1"),
                "T-Shirt",
                "A plain t-shirt",
                new BrandId("B-1"),
                new CategoryId("C-1"),
                "creator-1",
                List.of(new ProductFactoryParams.VariantSpec(
                        new VariantId("V-1"), "red-M", Map.of("color", "red", "size", "M"),
                        List.of(new ProductFactoryParams.SkuSpec(
                                new SkuId("S-1"), new SkuCode("SKU-1"), 250_000, Currency.VND)))),
                List.of()));

        Path fixture = JsonEventContract.writeFixture(new ProductCreated(product, FIXED_TIME), CONTRACTS);

        assertTrue(Files.readString(fixture).contains("\"SKU-1\""));
    }
}
