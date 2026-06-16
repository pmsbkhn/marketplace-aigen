package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Order aggregate identity (ULID/UUID). */
public class OrderId extends StringIdentity {

    public OrderId(String value) {
        super(value);
    }
}
