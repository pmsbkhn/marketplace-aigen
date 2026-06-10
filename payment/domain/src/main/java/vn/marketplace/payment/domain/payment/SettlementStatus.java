package vn.marketplace.payment.domain.payment;

/**
 * Settlement lifecycle: {@code PROCESSING → COMPLETED}. Completing requires the WORM document URI to
 * exist (tech-spec Invariant 3 — no document, no completed settlement).
 */
public enum SettlementStatus {
    PROCESSING,
    COMPLETED
}
