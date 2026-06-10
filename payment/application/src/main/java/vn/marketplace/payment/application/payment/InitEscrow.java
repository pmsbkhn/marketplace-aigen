package vn.marketplace.payment.application.payment;

/**
 * Use case: init the whole-cart escrow (called synchronously by the Checkout orchestrator).
 * Idempotent by {@code orderRef} — a saga retry returns the existing escrow/payment URL.
 */
public interface InitEscrow {

    InitEscrowResult execute(InitEscrowCmd cmd);
}
