package vn.marketplace.checkout.domain.checkout;

/**
 * The signals that advance {@link CheckoutState}. The synchronous orchestrator
 * ({@code SubmitCheckoutUc}) fires one as it enters each step; {@code FAIL} is fired when a step
 * fails. Kept as an explicit vocabulary so the same state graph could later be driven by an
 * event-driven saga without changing the states.
 */
public enum CheckoutTrigger {
    RESERVE,
    CREATE_ORDERS,
    INIT_ESCROW,
    REDIRECT,
    FAIL
}
