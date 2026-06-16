package vn.marketplace.payment.domain.payment;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Payment aggregate identity (ULID/UUID) — also the escrow id returned to Checkout. */
public class PaymentId extends StringIdentity {

    public PaymentId(String value) {
        super(value);
    }
}
