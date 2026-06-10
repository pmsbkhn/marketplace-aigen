package vn.marketplace.order.domain.orderlifecycle.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Published when an order reaches COMPLETED. Inventory consumes it to permanently deduct stock; Payment
 * consumes it for settlement/payout; Notification informs buyer + merchant.
 */
public class OrderCompleted extends AbstractDomainEvent {

    private final String orderId;
    private final String merchantId;
    private final List<Line> items;

    public OrderCompleted(Order order, DTime occurredAt) {
        super(DomainEventType.of("Order", "OrderCompleted"), occurredAt);
        this.orderId = order.id().value();
        this.merchantId = order.merchantId().value();
        this.items = order.items().stream()
                .map(i -> new Line(i.skuCode(), i.qty(), i.priceSnapshot().amount()))
                .toList();
        this.subject = this.orderId;
    }

    public String orderId() {
        return orderId;
    }

    public String merchantId() {
        return merchantId;
    }

    public List<Line> items() {
        return items;
    }

    /** One completed line: SKU + quantity + the snapshotted unit price. */
    public record Line(String sku, int qty, long priceSnapshot) {
    }
}
