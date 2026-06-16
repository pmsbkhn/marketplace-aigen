package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** The merchant the order belongs to (one order = one merchant, after Checkout splits the cart). */
public class MerchantId extends StringIdentity {

    public MerchantId(String value) {
        super(value);
    }
}
