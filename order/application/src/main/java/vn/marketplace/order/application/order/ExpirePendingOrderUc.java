package vn.marketplace.order.application.order;

import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.OrderStatus;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/**
 * FR13 auto-cancel, driven by the {@code Order/OrderPendingTimedOut} delayed event: cancels the
 * order with reason {@code PENDING_TIMEOUT} <em>iff it is still PENDING</em>. Any other state
 * means the timer is stale — payment arrived (TO_SHIP/...) or the order was already cancelled —
 * and the timer is skipped silently; this is what makes timers fire-and-forget (never cancelled,
 * at-least-once delivery absorbed). State-writing → {@link EventPublishHandler} so the published
 * {@code OrderCancelled} commits through the outbox with the state change.
 */
public class ExpirePendingOrderUc implements ExpirePendingOrder {

    public static final String REASON = "PENDING_TIMEOUT";
    public static final String TRIGGERED_BY = "system/auto-cancel";

    private final Repository<Order> orderRepository;

    public ExpirePendingOrderUc(Repository<Order> orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(ExpirePendingOrderCmd cmd) {
        Optional<Order> found = orderRepository.findById(new OrderId(cmd.orderId()));
        if (found.isEmpty() || found.get().status() != OrderStatus.PENDING) {
            return; // stale timer — paid, cancelled, or gone; nothing to expire
        }
        Order order = found.get();
        order.cancel(REASON, TRIGGERED_BY);
        orderRepository.save(order);
    }
}
