package vn.marketplace.inventory.adapter.stock.inbound.messaging;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.test.JsonEventContract;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Consumer side of the JSON contracts this service binds (fixtures written by the producers'
 * contract tests into {@code <repo>/contracts}).
 */
class ConsumedContractsBindingTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");

    @Test
    void bindsProductCreated() {
        ProductCreatedData data = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Catalog.ProductCreated.json"), ProductCreatedData.class);

        assertEquals("P-1", data.productId());
        assertEquals("M-1", data.merchantId());
        assertEquals(List.of("SKU-1"), data.skuCodes());
    }

    @Test
    void bindsOrderCompleted() {
        OrderCompletedData data = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Order.OrderCompleted.json"), OrderCompletedData.class);

        assertEquals("O-1", data.orderId());
        assertEquals("SKU-1", data.items().get(0).sku());
        assertEquals(2, data.items().get(0).qty());
    }
}
