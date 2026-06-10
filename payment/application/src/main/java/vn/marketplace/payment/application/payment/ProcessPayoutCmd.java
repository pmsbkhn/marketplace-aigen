package vn.marketplace.payment.application.payment;

/** Payout command — {@code orderId} locates the settlement (one settlement/payout per order). */
public record ProcessPayoutCmd(String orderId) {
}
