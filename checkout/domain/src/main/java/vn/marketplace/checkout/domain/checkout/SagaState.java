package vn.marketplace.checkout.domain.checkout;

/**
 * The linear, one-way saga progression (tech-spec Invariant 1):
 * {@code PRICING → RESERVING → ORDERING → ESCROWING → REDIRECTED}, with {@code FAILED} reachable
 * from any non-terminal state. REDIRECTED and FAILED are terminal — frozen forever.
 */
public enum SagaState {
    PRICING,
    RESERVING,
    ORDERING,
    ESCROWING,
    REDIRECTED,
    FAILED;

    public boolean isTerminal() {
        return this == REDIRECTED || this == FAILED;
    }
}
