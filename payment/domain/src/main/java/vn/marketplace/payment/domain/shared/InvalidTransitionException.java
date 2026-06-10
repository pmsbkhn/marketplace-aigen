package vn.marketplace.payment.domain.shared;

/**
 * Thrown when a state-machine transition is not allowed — e.g. {@code markFailed()} on a payment that
 * is already PAID (terminal), or {@code complete()} on a settlement that is not PROCESSING. A
 * {@link PaymentDomainException} of code {@link PaymentErrorCode#INVALID_TRANSITION}, but a distinct
 * type so callers/tests can target it (TC-PAY-02).
 */
public class InvalidTransitionException extends PaymentDomainException {

    public InvalidTransitionException(String from, String event) {
        super(PaymentErrorCode.INVALID_TRANSITION,
                "Illegal transition from " + from + " on event '" + event + "'");
    }
}
