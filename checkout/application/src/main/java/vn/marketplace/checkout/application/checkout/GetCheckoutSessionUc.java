package vn.marketplace.checkout.application.checkout;

import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

/**
 * Read-only session lookup. A foreign caller gets SESSION_NOT_FOUND (not a tenant error) — the 404
 * hides the key's existence, mirroring Order's anti-IDOR decision (ADR-7).
 */
public class GetCheckoutSessionUc implements GetCheckoutSession {

    private final CheckoutSessionPort sessions;

    public GetCheckoutSessionUc(CheckoutSessionPort sessions) {
        this.sessions = sessions;
    }

    @Override
    public CheckoutSession execute(String idempotencyKey, String callerBuyerId) {
        CheckoutSession session = sessions.find(idempotencyKey)
                .orElseThrow(() -> new CheckoutDomainException(CheckoutErrorCode.SESSION_NOT_FOUND));
        if (!session.buyerId().equals(callerBuyerId)) {
            throw new CheckoutDomainException(CheckoutErrorCode.SESSION_NOT_FOUND); // anti-IDOR 404
        }
        return session;
    }
}
