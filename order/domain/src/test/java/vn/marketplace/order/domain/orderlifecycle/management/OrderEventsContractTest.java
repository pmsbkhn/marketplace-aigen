package vn.marketplace.order.domain.orderlifecycle.management;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import tech.vsf.ptnt.msfw.test.JsonEventContract;
import vn.marketplace.order.domain.orderlifecycle.BuyerId;
import vn.marketplace.order.domain.orderlifecycle.MerchantId;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of Order's JSON contracts ({@code OrderCompleted}, {@code OrderCancelled},
 * {@code OrderPendingTimedOut}): writes the exact wire envelopes of representative events into
 * {@code <repo>/contracts}. Committed files — a git diff on one IS the contract-change signal;
 * consumers (payment, inventory, notification, and order itself for the timer) bind from them.
 */
class OrderEventsContractTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");
    private static final DTime FIXED_TIME = DTime.of(LocalDateTime.of(2026, 6, 12, 12, 0));

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Order order() {
        return Order.createPending(new OrderId("O-1"), "CHK-1", new BuyerId("B-1"), new MerchantId("M-1"),
                new ShippingAddress("Nguyen Van A", "0900000000", "1 Le Loi", "Ben Nghe", "Q1", "HCMC"),
                List.of(new Order.NewItem("OI-1", "P-1", "V-1", "SKU-1", "Shirt", 100, "VND", 2)),
                "B-1");
    }

    @Test
    void publishesTheOrderCompletedContract() throws Exception {
        Path fixture = JsonEventContract.writeFixture(new OrderCompleted(order(), FIXED_TIME), CONTRACTS);

        assertTrue(Files.readString(fixture).contains("\"priceSnapshot\""));
    }

    @Test
    void publishesTheOrderCancelledContract() throws Exception {
        Path fixture = JsonEventContract.writeFixture(
                new OrderCancelled(order(), "PENDING_TIMEOUT", FIXED_TIME), CONTRACTS);

        assertTrue(Files.readString(fixture).contains("\"PENDING_TIMEOUT\""));
    }

    @Test
    void publishesTheOrderPendingTimedOutContract() throws Exception {
        Path fixture = JsonEventContract.writeFixture(new OrderPendingTimedOut("O-1", FIXED_TIME,
                DTime.of(FIXED_TIME.value().plusMinutes(30))), CONTRACTS);

        assertTrue(Files.readString(fixture).contains("\"orderId\""));
    }
}
