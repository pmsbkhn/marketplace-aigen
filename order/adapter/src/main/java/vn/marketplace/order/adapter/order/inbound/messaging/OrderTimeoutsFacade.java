package vn.marketplace.order.adapter.order.inbound.messaging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.marketplace.order.application.order.ExpirePendingOrder;
import vn.marketplace.order.application.order.ExpirePendingOrderCmd;

/**
 * Handles our own delayed timers coming back through Kafka. FR13: a PENDING order whose payment
 * deadline passed is cancelled with {@code PENDING_TIMEOUT}; a stale timer (payment arrived, order
 * already cancelled) is skipped inside the use-case — nothing to coordinate here.
 */
@Service
public class OrderTimeoutsFacade {

    private final ExpirePendingOrder expirePendingOrder;

    public OrderTimeoutsFacade(ExpirePendingOrder expirePendingOrder) {
        this.expirePendingOrder = expirePendingOrder;
    }

    /** {@code Order/OrderPendingTimedOut} → cancel iff still PENDING. */
    @Transactional
    public void onPendingTimedOut(OrderPendingTimedOutData data) {
        expirePendingOrder.execute(new ExpirePendingOrderCmd(data.orderId()));
    }
}
