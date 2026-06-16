package vn.marketplace.payment.domain.payment;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Identity of the payout instruction inside the Settlement aggregate. */
public class PayoutId extends StringIdentity {

    public PayoutId(String value) {
        super(value);
    }
}
