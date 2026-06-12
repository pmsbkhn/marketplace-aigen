package vn.marketplace.payment.adapter.payment.dto;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.test.JsonEventContract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Consumer side of the {@code Order/OrderCompleted} JSON contract (fixture written by Order's
 * producer contract test into {@code <repo>/contracts}).
 */
class OrderCompletedContractBindingTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");

    @Test
    void bindsOrderCompleted() {
        OrderCompletedEvent event = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Order.OrderCompleted.json"), OrderCompletedEvent.class);

        assertEquals("O-1", event.orderId());
        assertEquals("M-1", event.merchantId());
        assertEquals(1, event.items().size());
        assertEquals("SKU-1", event.items().get(0).sku());
        assertEquals(2, event.items().get(0).qty());
        assertEquals(100, event.items().get(0).priceSnapshot());
        // eventId is not in the payload — it is stamped from the envelope id at consumption time.
        assertNull(event.eventId());
    }
}
