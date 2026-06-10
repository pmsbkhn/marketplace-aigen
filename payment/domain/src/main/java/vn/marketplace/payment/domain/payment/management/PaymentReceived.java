package vn.marketplace.payment.domain.payment.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Published when a payment reaches PAID (verified gateway webhook). Order consumes it to transition
 * each allocated order PENDING → TO_SHIP; Notification informs buyer + merchants.
 */
public class PaymentReceived extends AbstractDomainEvent {

    private final String paymentId;
    private final String orderRef;
    private final String gatewayTxnId;
    private final long amount;
    private final List<Allocation> allocations;

    public PaymentReceived(Payment payment, DTime occurredAt) {
        super(DomainEventType.of("Payment", "PaymentReceived"), occurredAt);
        this.paymentId = payment.id().value();
        this.orderRef = payment.orderRef();
        this.gatewayTxnId = payment.gatewayTxnId();
        this.amount = payment.amount().amount();
        this.allocations = payment.holds().stream()
                .map(h -> new Allocation(h.orderId(), h.merchantId().value(), h.amount().amount()))
                .toList();
        this.subject = this.paymentId;
    }

    public String paymentId() {
        return paymentId;
    }

    public String orderRef() {
        return orderRef;
    }

    public String gatewayTxnId() {
        return gatewayTxnId;
    }

    public long amount() {
        return amount;
    }

    public List<Allocation> allocations() {
        return allocations;
    }

    /** One paid order: orderId + merchant + the escrowed amount for that order. */
    public record Allocation(String orderId, String merchantId, long amount) {
    }
}
