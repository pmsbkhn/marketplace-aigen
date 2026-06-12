package vn.marketplace.order.domain.orderlifecycle.management;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DelayedEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Self-addressed payment-deadline timer (FR13 — auto-cancel PENDING past
 * {@code order.pending_expiry_min}). Published when a PENDING order is created, with
 * {@code deliverAfter} = creation + TTL: the outbox holds it until due, Kafka delivers it back to
 * this service, and the handler cancels the order <em>iff it is still PENDING</em> — a stale
 * timer arriving after payment (TO_SHIP) is skipped harmlessly, so timers never need cancelling.
 * Replaces the CronJob the techspec sketched (no table scan, no extra deployment).
 */
public class OrderPendingTimedOut extends AbstractDomainEvent implements DelayedEvent {

    private final String orderId;
    private final DTime deliverAfter;

    public OrderPendingTimedOut(String orderId, DTime occurredAt, DTime deliverAfter) {
        super(DomainEventType.of("Order", "OrderPendingTimedOut"), occurredAt);
        this.orderId = orderId;
        this.deliverAfter = deliverAfter;
        this.subject = orderId;
    }

    public String orderId() {
        return orderId;
    }

    @Override
    public DTime deliverAfter() {
        return deliverAfter;
    }
}
