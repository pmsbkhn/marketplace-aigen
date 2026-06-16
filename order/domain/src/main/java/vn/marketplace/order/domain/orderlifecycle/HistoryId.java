package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Identity of an {@code OrderStatusHistory} entry (ULID/UUID). */
public class HistoryId extends StringIdentity {

    public HistoryId(String value) {
        super(value);
    }
}
