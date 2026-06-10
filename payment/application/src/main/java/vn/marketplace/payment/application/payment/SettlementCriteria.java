package vn.marketplace.payment.application.payment;

import tech.vsf.ptnt.msfw.domain.core.Criteria;

/**
 * Lookup criteria for the {@code Repository<Settlement>} port. {@code orderId} is the settlement
 * idempotency key (one settlement per completed order).
 */
public record SettlementCriteria(String orderId, String merchantId) implements Criteria {

    public static SettlementCriteria byOrderId(String orderId) {
        return new SettlementCriteria(orderId, null);
    }
}
