package vn.marketplace.payment.adapter.payment.outbound.gateway;

/**
 * Thrown when something attempts to overwrite an existing settlement document — the WORM store
 * (S3 Object Lock in production) denies any overwrite/override (TC-PAY-INT-03). Mirrors the S3
 * {@code AccessDenied} error.
 */
public class WormAccessDeniedException extends RuntimeException {

    public WormAccessDeniedException(String documentKey) {
        super("AccessDenied: settlement document is write-once and already exists: " + documentKey);
    }
}
