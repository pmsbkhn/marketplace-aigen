package vn.marketplace.checkout.domain.shared;

/**
 * Thrown when a saga state-machine transition is not allowed — e.g. jumping PRICING → ORDERING
 * without RESERVING (TC-CHK-01), or mutating a terminal (REDIRECTED/FAILED) saga. A
 * {@link CheckoutDomainException} of code {@link CheckoutErrorCode#INVALID_TRANSITION}, but a
 * distinct type so callers/tests can target it.
 */
public class InvalidTransitionException extends CheckoutDomainException {

    public InvalidTransitionException(String from, String event) {
        super(CheckoutErrorCode.INVALID_TRANSITION,
                "Illegal saga transition from " + from + " on event '" + event + "'");
    }
}
