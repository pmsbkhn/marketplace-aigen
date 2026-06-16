package vn.marketplace.payment.domain.payment;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Settlement aggregate identity (ULID/UUID). */
public class SettlementId extends StringIdentity {

    public SettlementId(String value) {
        super(value);
    }
}
