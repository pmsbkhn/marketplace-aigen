package vn.marketplace.order.domain.orderlifecycle.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Published when an order reaches CANCELLED. Inventory consumes it to release any held stock; Payment
 * refunds if already paid; Notification informs the buyer.
 */
public class OrderCancelled extends AbstractDomainEvent {

    private final String orderId;
    private final String reason;
    private final List<Line> items;

    public OrderCancelled(Order order, String reason, DTime occurredAt) {
        super(DomainEventType.of("Order", "OrderCancelled"), occurredAt);
        this.orderId = order.id().value();
        this.reason = reason;
        this.items = order.items().stream()
                .map(i -> new Line(i.skuCode(), i.qty()))
                .toList();
        this.subject = this.orderId;
    }

    public String orderId() {
        return orderId;
    }

    public String reason() {
        return reason;
    }

    public List<Line> items() {
        return items;
    }

    /** One cancelled line: SKU + quantity (for stock release). */
    public record Line(String sku, int qty) {
    }
}
