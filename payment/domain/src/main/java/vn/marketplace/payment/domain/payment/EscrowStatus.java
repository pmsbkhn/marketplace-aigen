package vn.marketplace.payment.domain.payment;

/**
 * Escrow hold lifecycle: {@code HELD → RELEASED}, one-way (tech-spec Invariant 2). Release is legal
 * only when the owning payment is PAID and the order has completed.
 */
public enum EscrowStatus {
    HELD,
    RELEASED
}
