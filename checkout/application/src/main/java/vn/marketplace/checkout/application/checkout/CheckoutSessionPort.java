package vn.marketplace.checkout.application.checkout;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port to the checkout session store (Redis in production, in-memory stand-in on the
 * standalone profile). Two concerns, both TTL-bound:
 * <ul>
 *   <li><b>Idempotency fast-path</b> — terminal saga results cached ~30 min by key;</li>
 *   <li><b>Double-submit lock</b> — a short (~10 s) distributed lock per key so two concurrent
 *       requests with the same key cannot both run the saga (TC-CHK-04).</li>
 * </ul>
 * Losing the store loses idempotency only — reservations self-release via their Inventory TTL.
 */
public interface CheckoutSessionPort {

    Optional<CheckoutSession> find(String idempotencyKey);

    /** True if the lock was acquired; false → another request with this key is in flight. */
    boolean tryLock(String idempotencyKey);

    void unlock(String idempotencyKey);

    void saveCompleted(String idempotencyKey, String buyerId, List<String> orderIds,
                       String paymentUrl, long grandTotal);

    void saveFailed(String idempotencyKey, String buyerId, String reason);

    /** Cached terminal projection of one saga run. */
    record CheckoutSession(String idempotencyKey, String buyerId, String state,
                           List<String> orderIds, String paymentUrl, long grandTotal,
                           String failReason) {

        public static final String STATE_REDIRECTED = "REDIRECTED";
        public static final String STATE_FAILED = "FAILED";

        public boolean isRedirected() {
            return STATE_REDIRECTED.equals(state);
        }
    }
}
