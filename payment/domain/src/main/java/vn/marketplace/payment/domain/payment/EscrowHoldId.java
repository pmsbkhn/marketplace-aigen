package vn.marketplace.payment.domain.payment;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Identity of one escrow hold (the per-order allocation) inside the Payment aggregate. */
public class EscrowHoldId extends StringIdentity {

    public EscrowHoldId(String value) {
        super(value);
    }
}
