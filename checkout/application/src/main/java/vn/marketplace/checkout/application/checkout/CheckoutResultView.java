package vn.marketplace.checkout.application.checkout;

import java.util.List;

/** Result of a successful checkout: where to pay, the per-merchant orders, and the cart total. */
public record CheckoutResultView(String paymentUrl, List<String> orderIds, long grandTotalAmount) {
}
