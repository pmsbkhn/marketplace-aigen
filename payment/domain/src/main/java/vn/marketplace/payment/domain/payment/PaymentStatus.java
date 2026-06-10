package vn.marketplace.payment.domain.payment;

/**
 * Payment state machine: {@code PENDING → (PAID | FAILED)}. PAID and FAILED are terminal — frozen
 * forever, never back to PENDING (tech-spec Invariant 1).
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED;

    public boolean isTerminal() {
        return this != PENDING;
    }
}
