package vn.marketplace.order.application.order;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.management.Order;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

/**
 * Cancels an order. The aggregate enforces that only PENDING/TO_SHIP may cancel (SHIPPED/COMPLETED →
 * {@code InvalidTransitionException}), is idempotent if already CANCELLED, and publishes
 * {@code OrderCancelled}. State-writing → {@link EventPublishHandler}.
 */
public class CancelOrderUc implements CancelOrder {

    private final Repository<Order> orderRepository;

    public CancelOrderUc(Repository<Order> orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(CancelOrderCmd cmd) {
        Order order = orderRepository.findById(new OrderId(cmd.orderId()))
                .orElseThrow(() -> new OrderDomainException(OrderErrorCode.ORDER_NOT_FOUND));

        order.cancel(cmd.reason(), cmd.triggeredBy());
        orderRepository.save(order);
    }
}
