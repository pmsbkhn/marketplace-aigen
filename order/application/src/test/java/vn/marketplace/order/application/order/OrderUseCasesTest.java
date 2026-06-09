package vn.marketplace.order.application.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.OrderItemInput;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.ShippingAddressInput;
import vn.marketplace.order.domain.orderlifecycle.management.OrderCompleted;
import vn.marketplace.order.domain.shared.Actor;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;
import vn.marketplace.order.domain.shared.Role;

class OrderUseCasesTest {

    private static final ShippingAddressInput ADDRESS =
            new ShippingAddressInput("Nguyen Van A", "0900000000", "1 Le Loi", "Ben Nghe", "Q1", "HCMC");

    private InMemoryOrderRepository repository;
    private CreatePendingOrderUc createUc;
    private TransitionOrderUc transitionUc;
    private CancelOrderUc cancelUc;
    private GetOrderUc getUc;

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        createUc = new CreatePendingOrderUc(repository);
        transitionUc = new TransitionOrderUc(repository);
        cancelUc = new CancelOrderUc(repository);
        getUc = new GetOrderUc(repository);
    }

    private String createOrder(String checkoutRef) {
        return createUc.execute(new CreatePendingOrderCmd(checkoutRef, "B-1", "M-1", ADDRESS,
                List.of(new OrderItemInput("P-1", "V-1", "SKU-1", "Shirt", 100, "VND", 2)))).orderId();
    }

    @Test
    void createComputesTotalAndIsIdempotentByCheckoutRef() {
        String id1 = createOrder("CHK-1");
        String id2 = createOrder("CHK-1"); // replay → same order

        assertEquals(id1, id2);
        assertEquals(1, repository.store.size());
        assertEquals(200, repository.store.get(id1).totalAmount().amount());
    }

    @Test
    void fullLifecycleToCompletedPublishesOrderCompleted() {
        String id = createOrder("CHK-1");

        transitionUc.execute(new TransitionOrderCmd(id, TransitionEvent.PAYMENT_RECEIVED, "system", null, "e1"));
        assertEquals("TO_SHIP", repository.store.get(id).status().name());

        transitionUc.execute(new TransitionOrderCmd(id, TransitionEvent.MERCHANT_SHIP, "M-1", "TRACK-1", null));
        assertEquals("SHIPPED", repository.store.get(id).status().name());

        DomainEventPublisher.clear();
        transitionUc.execute(new TransitionOrderCmd(id, TransitionEvent.BUYER_CONFIRM, "B-1", null, null));
        assertEquals("COMPLETED", repository.store.get(id).status().name());
        assertInstanceOf(OrderCompleted.class, DomainEventPublisher.getEvents().get(0));
    }

    @Test
    void cancelFromPendingPublishesOrderCancelled() {
        String id = createOrder("CHK-1");

        cancelUc.execute(new CancelOrderCmd(id, "system", "PAYMENT_TIMEOUT"));

        assertEquals("CANCELLED", repository.store.get(id).status().name());
        assertEquals("OrderCancelled", DomainEventPublisher.getEvents().get(0).getClass().getSimpleName());
    }

    @Test
    void getOrderHidesOtherTenantsAs404() {
        String id = createOrder("CHK-1"); // buyer B-1, merchant M-1

        OrderView own = getUc.execute(new GetOrderCmd(id, new Actor("B-1", Role.BUYER)));
        assertEquals("SKU-1", own.items().get(0).skuCode());

        OrderDomainException ex = assertThrows(OrderDomainException.class,
                () -> getUc.execute(new GetOrderCmd(id, new Actor("B-2", Role.BUYER))));
        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, ex.orderErrorCode());
    }
}
