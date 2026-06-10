package vn.marketplace.order.adapter.order.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.marketplace.order.application.order.CancelOrder;
import vn.marketplace.order.application.order.CancelOrderCmd;
import vn.marketplace.order.application.order.CreatePendingOrder;
import vn.marketplace.order.application.order.CreatePendingOrderCmd;
import vn.marketplace.order.application.order.GetOrder;
import vn.marketplace.order.application.order.GetOrderCmd;
import vn.marketplace.order.application.order.OrderIdView;
import vn.marketplace.order.application.order.OrderView;
import vn.marketplace.order.application.order.TransitionEvent;
import vn.marketplace.order.application.order.TransitionOrder;
import vn.marketplace.order.application.order.TransitionOrderCmd;
import vn.marketplace.order.domain.shared.Actor;

/** Transactional application boundary for the order use-cases. */
@Service
public class OrderFacade {

    private final CreatePendingOrder createPendingOrder;
    private final TransitionOrder transitionOrder;
    private final CancelOrder cancelOrder;
    private final GetOrder getOrder;

    public OrderFacade(CreatePendingOrder createPendingOrder,
                       TransitionOrder transitionOrder,
                       CancelOrder cancelOrder,
                       GetOrder getOrder) {
        this.createPendingOrder = createPendingOrder;
        this.transitionOrder = transitionOrder;
        this.cancelOrder = cancelOrder;
        this.getOrder = getOrder;
    }

    @Transactional
    public OrderIdView create(CreatePendingOrderCmd cmd) {
        return createPendingOrder.execute(cmd);
    }

    @Transactional(readOnly = true)
    public OrderView get(String orderId, Actor caller) {
        return getOrder.execute(new GetOrderCmd(orderId, caller));
    }

    @Transactional
    public void ship(String orderId, String trackingNumber, Actor actor) {
        transitionOrder.execute(new TransitionOrderCmd(orderId, TransitionEvent.MERCHANT_SHIP,
                actor.id(), trackingNumber, null));
    }

    @Transactional
    public void confirmDelivery(String orderId, Actor actor) {
        transitionOrder.execute(new TransitionOrderCmd(orderId, TransitionEvent.BUYER_CONFIRM,
                actor.id(), null, null));
    }

    @Transactional
    public void paymentReceived(String orderId, String eventId) {
        transitionOrder.execute(new TransitionOrderCmd(orderId, TransitionEvent.PAYMENT_RECEIVED,
                "payment-service", null, eventId));
    }

    @Transactional
    public void cancel(String orderId, String reason, Actor actor) {
        cancelOrder.execute(new CancelOrderCmd(orderId, actor.id(), reason));
    }

    @Transactional
    public void cancelBySystem(String orderId, String reason) {
        cancelOrder.execute(new CancelOrderCmd(orderId, "system", reason));
    }
}
