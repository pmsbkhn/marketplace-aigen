package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** The buyer who placed the order. */
public class BuyerId extends StringIdentity {

    public BuyerId(String value) {
        super(value);
    }
}
