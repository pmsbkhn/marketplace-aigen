package vn.marketplace.notification.adapter.delivery.inbound.messaging;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.test.JsonEventContract;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Consumer side of the {@code Payment/PaymentReceived} JSON contract (fixture written by
 * Payment's producer contract test into {@code <repo>/contracts}).
 */
class PaymentReceivedContractBindingTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");

    @Test
    void bindsPaymentReceived() {
        PaymentReceivedData data = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Payment.PaymentReceived.json"), PaymentReceivedData.class);

        assertEquals("PAY-1", data.paymentId());
        assertEquals("CHK-1", data.orderRef());
        assertEquals(130_000, data.amount());
        assertEquals("O-1", data.allocations().get(0).orderId());
    }
}
