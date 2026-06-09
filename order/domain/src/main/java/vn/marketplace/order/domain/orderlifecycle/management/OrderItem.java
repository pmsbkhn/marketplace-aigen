package vn.marketplace.order.domain.orderlifecycle.management;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.order.domain.orderlifecycle.Money;
import vn.marketplace.order.domain.orderlifecycle.OrderItemId;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

/**
 * A line in an {@link Order}, inside the aggregate boundary. The {@code priceSnapshot} is captured at
 * order creation and is <strong>immutable</strong> — there is no mutator, so a later Catalog price
 * change never affects an existing order. {@code subtotal = priceSnapshot × qty}.
 *
 * <p>Invariants: {@code qty > 0} (INVALID_QUANTITY), {@code priceSnapshot > 0} (INVALID_PRICE).
 */
public class OrderItem implements Entity {

    private final OrderItemId id;
    private final String productId;
    private final String variantId;
    private final String skuCode;
    private final String productName;
    private final Money priceSnapshot;
    private final int qty;
    private final Money subtotal;

    OrderItem(OrderItemId id, String productId, String variantId, String skuCode,
              String productName, Money priceSnapshot, int qty) {
        if (qty <= 0) {
            throw new OrderDomainException(OrderErrorCode.INVALID_QUANTITY);
        }
        if (priceSnapshot.amount() <= 0) {
            throw new OrderDomainException(OrderErrorCode.INVALID_PRICE);
        }
        this.id = id;
        this.productId = productId;
        this.variantId = variantId;
        this.skuCode = skuCode;
        this.productName = productName;
        this.priceSnapshot = priceSnapshot;
        this.qty = qty;
        this.subtotal = priceSnapshot.times(qty);
    }

    static OrderItem fromMemento(Order.OrderItemMemento m) {
        return new OrderItem(new OrderItemId(m.orderItemId()), m.productId(), m.variantId(), m.skuCode(),
                m.productName(), Money.of(m.priceAmount(), vn.marketplace.order.domain.orderlifecycle.Currency.of(m.currency())), m.qty());
    }

    public OrderItemId id() {
        return id;
    }

    public String productId() {
        return productId;
    }

    public String variantId() {
        return variantId;
    }

    public String skuCode() {
        return skuCode;
    }

    public String productName() {
        return productName;
    }

    public Money priceSnapshot() {
        return priceSnapshot;
    }

    public int qty() {
        return qty;
    }

    public Money subtotal() {
        return subtotal;
    }
}
