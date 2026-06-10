package vn.marketplace.payment.application.payment;

/** Lookup command — by checkout {@code orderRef}. */
public record GetPaymentCmd(String orderRef) {
}
