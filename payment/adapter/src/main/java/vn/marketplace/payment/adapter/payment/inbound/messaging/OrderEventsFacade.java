package vn.marketplace.payment.adapter.payment.inbound.messaging;

import org.springframework.stereotype.Service;
import vn.marketplace.payment.adapter.payment.dto.OrderCompletedEvent;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;

/**
 * Event-subscriber boundary — {@code Order/OrderCompleted} triggers settlement + payout.
 * Delegates to {@link PaymentFacade#settleAndPayout} (one transaction: a payout failure rolls the
 * settlement back, the redelivered event retries both; idempotent by {@code orderId}). A void
 * wrapper is required because pipeline targets must not return a value.
 */
@Service
public class OrderEventsFacade {

    private final PaymentFacade paymentFacade;

    public OrderEventsFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public void onOrderCompleted(OrderCompletedEvent event) {
        paymentFacade.settleAndPayout(event);
    }
}
