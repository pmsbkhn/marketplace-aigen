package vn.marketplace.checkout.application.checkout;

import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;

/** Use case: read one checkout session — owner-only ({@code userId == session.buyerId}). */
public interface GetCheckoutSession {

    CheckoutSession execute(String idempotencyKey, String callerBuyerId);
}
