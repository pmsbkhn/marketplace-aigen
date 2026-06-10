package vn.marketplace.payment.domain.payment;

/**
 * Payout lifecycle: {@code PENDING → SUBMITTED}. Submitting twice is a no-op — a duplicate bank
 * instruction is real money sent twice (TC-PAY-06).
 */
public enum PayoutStatus {
    PENDING,
    SUBMITTED
}
