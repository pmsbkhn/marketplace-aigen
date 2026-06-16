package vn.marketplace.payment.domain.payment;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Merchant identity (tenant scope) — logical reference to the Identity context, no physical FK. */
public class MerchantId extends StringIdentity {

    public MerchantId(String value) {
        super(value);
    }
}
