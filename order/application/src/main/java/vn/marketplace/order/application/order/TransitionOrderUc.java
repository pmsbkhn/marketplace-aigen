package vn.marketplace.order.application.order;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.management.Order;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

/**
 * Drives a forward transition through the order state machine. The legal-transition rules and any
 * resulting domain event ({@code OrderCompleted} on COMPLETED) live in the aggregate. State-writing →
 * {@link EventPublishHandler} (outbox path).
 */
public class TransitionOrderUc implements TransitionOrder {

    private final Repository<Order> orderRepository;

    public TransitionOrderUc(Repository<Order> orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(TransitionOrderCmd cmd) {
        Order order = orderRepository.findById(new OrderId(cmd.orderId()))
                .orElseThrow(() -> new OrderDomainException(OrderErrorCode.ORDER_NOT_FOUND));

        switch (cmd.event()) {
            case PAYMENT_RECEIVED -> order.receivePayment(cmd.triggeredBy());
            case MERCHANT_SHIP -> order.ship(cmd.trackingNumber(), cmd.triggeredBy());
            case BUYER_CONFIRM, AUTO_COMPLETE -> order.complete(cmd.triggeredBy());
        }

        orderRepository.save(order);
    }
}
