package vn.marketplace.order.domain.orderlifecycle.management;

import java.time.LocalDateTime;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.order.domain.orderlifecycle.HistoryId;
import vn.marketplace.order.domain.orderlifecycle.OrderStatus;

/**
 * An append-only audit record of one status transition. <strong>Fully immutable</strong> — every field
 * is {@code final} and there are no mutators, so a recorded history entry can never be altered
 * (audit-trail immutability invariant).
 */
public final class OrderStatusHistory implements Entity {

    private final HistoryId id;
    private final OrderStatus fromStatus; // null for the initial PENDING entry
    private final OrderStatus toStatus;
    private final String triggeredBy;
    private final String reason;
    private final LocalDateTime timestamp;

    OrderStatusHistory(HistoryId id, OrderStatus fromStatus, OrderStatus toStatus,
                       String triggeredBy, String reason, LocalDateTime timestamp) {
        this.id = id;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.triggeredBy = triggeredBy;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    static OrderStatusHistory fromMemento(Order.StatusHistoryMemento m) {
        return new OrderStatusHistory(new HistoryId(m.historyId()),
                m.fromStatus() == null ? null : OrderStatus.valueOf(m.fromStatus()),
                OrderStatus.valueOf(m.toStatus()), m.triggeredBy(), m.reason(), m.timestamp());
    }

    public HistoryId id() {
        return id;
    }

    public OrderStatus fromStatus() {
        return fromStatus;
    }

    public OrderStatus toStatus() {
        return toStatus;
    }

    public String triggeredBy() {
        return triggeredBy;
    }

    public String reason() {
        return reason;
    }

    public LocalDateTime timestamp() {
        return timestamp;
    }
}
