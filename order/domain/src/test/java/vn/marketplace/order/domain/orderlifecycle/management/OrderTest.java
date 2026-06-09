package vn.marketplace.order.domain.orderlifecycle.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.order.domain.orderlifecycle.BuyerId;
import vn.marketplace.order.domain.orderlifecycle.MerchantId;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.shared.InvalidTransitionException;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

class OrderTest {

    private static final ShippingAddress ADDRESS =
            new ShippingAddress("Nguyen Van A", "0900000000", "1 Le Loi", "Ben Nghe", "Q1", "HCMC");

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Order.NewItem item(String sku, long price, int qty) {
        return new Order.NewItem("oi-" + sku, "P-1", "V-1", sku, "Product " + sku, price, "VND", qty);
    }

    private Order pendingOrder(Order.NewItem... items) {
        return Order.createPending(new OrderId("O-1"), "CHK-1", new BuyerId("B-1"), new MerchantId("M-1"),
                ADDRESS, List.of(items), "B-1");
    }

    @Test
    void skipStateTransitionThrows() { // TC-ORD-01: PENDING → SHIPPED (skip TO_SHIP)
        Order order = pendingOrder(item("SKU-1", 100, 1));

        assertThrows(InvalidTransitionException.class, () -> order.ship("TRACK-1", "M-1"));
    }

    @Test
    void cancellingShippedOrCompletedThrows() { // TC-ORD-02
        Order shipped = pendingOrder(item("SKU-1", 100, 1));
        shipped.receivePayment("system");
        shipped.ship("TRACK-1", "M-1");
        assertThrows(InvalidTransitionException.class, () -> shipped.cancel("changed mind", "B-1"));

        shipped.complete("B-1");
        assertThrows(InvalidTransitionException.class, () -> shipped.cancel("too late", "B-1"));
    }

    @Test
    void emptyOrderThrowsEmptyCart() { // TC-ORD-03
        OrderDomainException ex = assertThrows(OrderDomainException.class, this::pendingOrder);
        assertEquals(OrderErrorCode.EMPTY_CART, ex.orderErrorCode());
    }

    @Test
    void totalAmountIsSumOfLineSubtotals() { // TC-ORD-04: (2×100) + (1×50) = 250
        Order order = pendingOrder(item("SKU-1", 100, 2), item("SKU-2", 50, 1));

        assertEquals(250, order.totalAmount().amount());
    }

    @Test
    void statusHistoryIsImmutable() { // TC-ORD-05: no mutators, all fields final
        for (Field f : OrderStatusHistory.class.getDeclaredFields()) {
            assertTrue(Modifier.isFinal(f.getModifiers()),
                    "OrderStatusHistory field must be final: " + f.getName());
        }
        for (Method m : OrderStatusHistory.class.getDeclaredMethods()) {
            assertTrue(!m.getName().startsWith("set"),
                    "OrderStatusHistory must have no setter: " + m.getName());
        }
    }

    @Test
    void happyPathPublishesOrderCompleted() {
        Order order = pendingOrder(item("SKU-1", 100, 2));
        order.receivePayment("system");
        order.ship("TRACK-1", "M-1");
        DomainEventPublisher.clear();

        order.complete("B-1");

        assertEquals(1, DomainEventPublisher.getEvents().size());
        assertInstanceOf(OrderCompleted.class, DomainEventPublisher.getEvents().get(0));
    }

    @Test
    void cancelFromPendingPublishesOrderCancelledAndIsIdempotent() {
        Order order = pendingOrder(item("SKU-1", 100, 1));

        order.cancel("payment failed", "system");
        assertInstanceOf(OrderCancelled.class, DomainEventPublisher.getEvents().get(0));

        DomainEventPublisher.clear();
        order.cancel("again", "system"); // idempotent no-op
        assertTrue(DomainEventPublisher.getEvents().isEmpty());
    }
}
