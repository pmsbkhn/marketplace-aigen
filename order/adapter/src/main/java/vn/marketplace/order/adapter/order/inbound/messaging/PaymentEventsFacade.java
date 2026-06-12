package vn.marketplace.order.adapter.order.inbound.messaging;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.vsf.ptnt.msfw.event.causation.EventCausation;
import vn.marketplace.order.application.order.CancelOrder;
import vn.marketplace.order.application.order.CancelOrderCmd;
import vn.marketplace.order.application.order.TransitionEvent;
import vn.marketplace.order.application.order.TransitionOrder;
import vn.marketplace.order.application.order.TransitionOrderCmd;

/**
 * Event-subscriber boundary (tech spec: {@code event-subscriber} / Order Worker) — maps Payment's
 * events to the order state machine. One payment may cover several orders, so each event fans out
 * per order id; {@code eventId} for application-level idempotency comes from {@link EventCausation}.
 */
@Service
public class PaymentEventsFacade {

    private static final String TRIGGERED_BY = "payment-event";

    private final TransitionOrder transitionOrder;
    private final CancelOrder cancelOrder;

    public PaymentEventsFacade(TransitionOrder transitionOrder, CancelOrder cancelOrder) {
        this.transitionOrder = transitionOrder;
        this.cancelOrder = cancelOrder;
    }

    /** {@code Payment/PaymentReceived} → each allocated order PENDING → TO_SHIP. */
    @Transactional
    public void onPaymentReceived(PaymentReceivedData data) {
        String eventId = consumedEventId();
        for (PaymentReceivedData.Allocation allocation : allocations(data)) {
            transitionOrder.execute(new TransitionOrderCmd(
                    allocation.orderId(), TransitionEvent.PAYMENT_RECEIVED, TRIGGERED_BY, null, eventId));
        }
    }

    /** {@code Payment/PaymentFailed} → cancel each pending order of the checkout. */
    @Transactional
    public void onPaymentFailed(PaymentFailedData data) {
        List<String> orderIds = data.orderIds() == null ? List.of() : data.orderIds();
        for (String orderId : orderIds) {
            cancelOrder.execute(new CancelOrderCmd(orderId, TRIGGERED_BY, "PAYMENT_FAILED: " + data.reason()));
        }
    }

    private static List<PaymentReceivedData.Allocation> allocations(PaymentReceivedData data) {
        return data.allocations() == null ? List.of() : data.allocations();
    }

    private static String consumedEventId() {
        return EventCausation.current().map(EventCausation.Causation::causationId).orElse(null);
    }
}
