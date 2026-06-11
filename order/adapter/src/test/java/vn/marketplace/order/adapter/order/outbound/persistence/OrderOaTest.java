package vn.marketplace.order.adapter.order.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.order.domain.orderlifecycle.BuyerId;
import vn.marketplace.order.domain.orderlifecycle.MerchantId;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.OrderStatus;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.orderlifecycle.management.Order;
import vn.marketplace.order.domain.orderlifecycle.management.OrderItem;
import vn.marketplace.order.domain.orderlifecycle.management.OrderStatusHistory;

/**
 * Pins the {@link OrderOa} ↔ database contract on a real (H2) JPA session:
 * <ul>
 *   <li>surrogate-key threading on insert, and round-tripping of both child collections;</li>
 *   <li>detached-entity merge on update keeps child semantics — items stay value-identical and the
 *       status history stays append-only and ordered across multiple save cycles
 *       ({@code orphanRemoval} rebuild must not lose, duplicate, or reorder rows);</li>
 *   <li>upsert-by-orderId: a fresh aggregate ({@code _id == null}) for an existing order updates
 *       the row instead of violating the unique index (legacy adapter semantics);</li>
 *   <li>{@code Criteria} DSL filters and real DB paging via the base {@code findBy};</li>
 *   <li>delete by domain identity.</li>
 * </ul>
 */
@DataJpaTest
class OrderOaTest {

    @Autowired
    private OrderJpaRepository jpa;

    @Autowired
    private TestEntityManager em;

    private OrderOa oa;

    @BeforeEach
    void setUp() {
        oa = new OrderOa(jpa);
        DomainEventPublisher.clear();
    }

    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private static Order newOrder(String orderId, String checkoutRef, String buyerId) {
        return Order.createPending(new OrderId(orderId), checkoutRef, new BuyerId(buyerId), new MerchantId("M-1"),
                new ShippingAddress("Nguyen Van A", "0900000000", "1 Le Loi", "Ben Nghe", "Q1", "HCMC"),
                List.of(new Order.NewItem("OI-" + orderId, "P-1", "V-1", "SKU-1", "Shirt", 100, "VND", 2)),
                buyerId);
    }

    /** Force SQL and detach everything so the next adapter call works on fresh rows. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void saveNewThreadsSurrogateIdAndRoundTripsChildren() {
        Order order = newOrder("O-1", "CHK-1", "B-1");
        oa.save(order);

        assertNotNull(order._id(), "save must thread the surrogate id back");
        flushAndClear();

        Order loaded = oa.findById(new OrderId("O-1")).orElseThrow();
        assertEquals(order._id(), loaded._id());
        assertEquals(OrderStatus.PENDING, loaded.status());
        assertEquals("CHK-1", loaded.checkoutRef());
        assertEquals(200, loaded.totalAmount().amount());
        assertEquals("HCMC", loaded.shippingAddress().city());

        assertEquals(1, loaded.items().size());
        OrderItem item = loaded.items().get(0);
        assertEquals("OI-O-1", item.id().value());
        assertEquals("SKU-1", item.skuCode());
        assertEquals(100, item.priceSnapshot().amount());
        assertEquals(2, item.qty());

        assertEquals(1, loaded.statusHistory().size());
        assertNull(loaded.statusHistory().get(0).fromStatus());
        assertEquals(OrderStatus.PENDING, loaded.statusHistory().get(0).toStatus());
    }

    @Test
    void detachedUpdatesKeepItemsIdenticalAndHistoryAppendOnlyOrdered() {
        oa.save(newOrder("O-1", "CHK-1", "B-1"));
        flushAndClear();

        Order paid = oa.findById(new OrderId("O-1")).orElseThrow();
        paid.receivePayment("system");
        oa.save(paid); // _id set → detached merge; children rebuilt from the memento
        flushAndClear();

        Order shipped = oa.findById(new OrderId("O-1")).orElseThrow();
        shipped.ship("TRACK-1", "M-1");
        oa.save(shipped);
        flushAndClear();

        Order reloaded = oa.findById(new OrderId("O-1")).orElseThrow();
        assertEquals(OrderStatus.SHIPPED, reloaded.status());
        assertEquals("TRACK-1", reloaded.trackingNumber());

        // items: value-identical, not lost or duplicated by the orphanRemoval rebuild
        assertEquals(1, reloaded.items().size());
        assertEquals("OI-O-1", reloaded.items().get(0).id().value());
        assertEquals(100, reloaded.items().get(0).priceSnapshot().amount());

        // history: append-only and ordered across both detached save cycles
        List<OrderStatusHistory> history = reloaded.statusHistory();
        assertEquals(3, history.size());
        assertNull(history.get(0).fromStatus());
        assertEquals(OrderStatus.PENDING, history.get(0).toStatus());
        assertEquals(OrderStatus.PENDING, history.get(1).fromStatus());
        assertEquals(OrderStatus.TO_SHIP, history.get(1).toStatus());
        assertEquals(OrderStatus.TO_SHIP, history.get(2).fromStatus());
        assertEquals(OrderStatus.SHIPPED, history.get(2).toStatus());

        assertEquals(1, jpa.count(), "still exactly one orders row");
    }

    @Test
    void freshAggregateForExistingOrderIdUpdatesTheRowInsteadOfInserting() {
        oa.save(newOrder("O-1", "CHK-1", "B-1"));
        flushAndClear();

        // Rebuild the aggregate from scratch (no _id — e.g. mapped from an external snapshot)
        Order.Memento m = oa.findById(new OrderId("O-1")).orElseThrow().snapshot();
        Order fresh = Order.restore(new Order.Memento(null, m.orderId(), m.checkoutRef(), m.buyerId(),
                m.merchantId(), OrderStatus.TO_SHIP.name(), m.totalAmount(), m.currency(), m.shippingAddress(),
                m.trackingNumber(), m.cancelReason(), m.createdAt(), m.updatedAt(), m.items(), m.statusHistory()));
        oa.save(fresh);
        flushAndClear();

        assertEquals(1, jpa.count(), "upsert by orderId must not insert a second row");
        assertEquals(OrderStatus.TO_SHIP, oa.findById(new OrderId("O-1")).orElseThrow().status());
    }

    @Test
    void criteriaFindByFiltersOnRootAttributesAndPagesInTheDatabase() {
        oa.save(newOrder("O-1", "CHK-1", "B-1"));
        oa.save(newOrder("O-2", "CHK-2", "B-1"));
        oa.save(newOrder("O-3", "CHK-3", "B-1"));
        oa.save(newOrder("O-4", "CHK-4", "B-2"));
        flushAndClear();

        assertEquals(3, oa.findBy(Criteria.where("buyerId").eq("B-1")).size());
        assertEquals(1, oa.findBy(Criteria.where("checkoutRef").eq("CHK-2")).size());
        assertEquals(1, oa.findBy(Criteria.where("buyerId").eq("B-1")
                .and(Criteria.where("checkoutRef").eq("CHK-3"))).size());
        assertEquals(4, oa.findBy(Criteria.matchAll()).size());

        PagedSearchResult<Order> page0 = oa.findBy(Criteria.where("buyerId").eq("B-1"), Pagination.of(0, 2));
        assertEquals(2, page0.content().size());
        assertEquals(0, page0.page());
        assertEquals(1, page0.nextPage().orElseThrow(), "3 matches with size 2 → a next page exists");

        PagedSearchResult<Order> page1 = oa.findBy(Criteria.where("buyerId").eq("B-1"), Pagination.of(1, 2));
        assertEquals(1, page1.content().size());
        assertTrue(page1.nextPage().isEmpty(), "last page must not advertise a next page");
    }

    @Test
    void deleteByDomainIdentityRemovesTheAggregate() {
        oa.save(newOrder("O-1", "CHK-1", "B-1"));
        flushAndClear();

        oa.delete(new OrderId("O-1"));
        flushAndClear();

        assertTrue(oa.findById(new OrderId("O-1")).isEmpty());
        assertEquals(0, jpa.count());
    }
}
