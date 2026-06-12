package vn.marketplace.order.adapter.order.inbound.messaging;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.test.JsonEventContract;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Consumer side of the JSON contracts this service binds (fixtures written by the producers'
 * contract tests into {@code <repo>/contracts}). Value assertions — not just the bind — are what
 * catch a silently renamed producer field on these ignoreUnknown DTOs.
 */
class PaymentContractBindingTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");

    @Test
    void bindsPaymentReceived() {
        PaymentReceivedData data = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Payment.PaymentReceived.json"), PaymentReceivedData.class);

        assertEquals("PAY-1", data.paymentId());
        assertEquals("CHK-1", data.orderRef());
        assertEquals(1, data.allocations().size());
        assertEquals("O-1", data.allocations().get(0).orderId());
        assertEquals("M-1", data.allocations().get(0).merchantId());
        assertEquals(130_000, data.allocations().get(0).amount());
    }

    @Test
    void bindsPaymentFailed() {
        PaymentFailedData data = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Payment.PaymentFailed.json"), PaymentFailedData.class);

        assertEquals("PAY-1", data.paymentId());
        assertEquals("GATEWAY_TIMEOUT", data.reason());
        assertEquals(java.util.List.of("O-1"), data.orderIds());
    }

    @Test
    void bindsOurOwnPendingTimer() {
        OrderPendingTimedOutData data = JsonEventContract.assertBinds(
                CONTRACTS.resolve("Order.OrderPendingTimedOut.json"), OrderPendingTimedOutData.class);

        assertEquals("O-1", data.orderId());
    }
}
