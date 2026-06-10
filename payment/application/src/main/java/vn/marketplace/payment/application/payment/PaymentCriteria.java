package vn.marketplace.payment.application.payment;

import tech.vsf.ptnt.msfw.domain.core.Criteria;

/**
 * Lookup criteria for the {@code Repository<Payment>} port. {@code orderRef} drives init-escrow
 * idempotency; {@code orderId} locates the payment owning an order's escrow hold; {@code gatewayTxnId}
 * drives webhook dedup (the DB UNIQUE constraint is the final authority).
 */
public record PaymentCriteria(String orderRef, String orderId, String gatewayTxnId) implements Criteria {

    public static PaymentCriteria byOrderRef(String orderRef) {
        return new PaymentCriteria(orderRef, null, null);
    }

    public static PaymentCriteria byOrderId(String orderId) {
        return new PaymentCriteria(null, orderId, null);
    }

    public static PaymentCriteria byGatewayTxnId(String gatewayTxnId) {
        return new PaymentCriteria(null, null, gatewayTxnId);
    }
}
