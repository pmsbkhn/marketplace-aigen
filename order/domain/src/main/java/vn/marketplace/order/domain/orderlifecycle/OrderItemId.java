package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Identity of an {@code OrderItem} (ULID/UUID). */
public class OrderItemId extends StringIdentity {

    public OrderItemId(String value) {
        super(value);
    }
}
