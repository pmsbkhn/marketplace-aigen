package vn.marketplace.payment.domain.payment.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Published when a payment reaches FAILED. Order consumes it to cancel the pending orders of this
 * checkout (which in turn releases the Inventory reservations).
 */
public class PaymentFailed extends AbstractDomainEvent {

    private final String paymentId;
    private final String orderRef;
    private final String reason;
    private final List<String> orderIds;

    public PaymentFailed(Payment payment, String reason, DTime occurredAt) {
        super(DomainEventType.of("Payment", "PaymentFailed"), occurredAt);
        this.paymentId = payment.id().value();
        this.orderRef = payment.orderRef();
        this.reason = reason;
        this.orderIds = payment.holds().stream().map(EscrowHold::orderId).toList();
        this.subject = this.paymentId;
    }

    public String paymentId() {
        return paymentId;
    }

    public String orderRef() {
        return orderRef;
    }

    public String reason() {
        return reason;
    }

    public List<String> orderIds() {
        return orderIds;
    }
}
