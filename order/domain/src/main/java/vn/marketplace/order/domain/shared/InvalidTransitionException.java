package vn.marketplace.order.domain.shared;

import vn.marketplace.order.domain.orderlifecycle.OrderStatus;

/**
 * Thrown when a state-machine transition is not allowed (e.g. PENDING → SHIPPED skipping TO_SHIP, or
 * cancelling a SHIPPED/COMPLETED order). An {@link OrderDomainException} of code
 * {@link OrderErrorCode#INVALID_TRANSITION}, but a distinct type so callers/tests can target it.
 */
public class InvalidTransitionException extends OrderDomainException {

    public InvalidTransitionException(OrderStatus from, String event) {
        super(OrderErrorCode.INVALID_TRANSITION,
                "Illegal transition from " + from + " on event '" + event + "'");
    }
}
