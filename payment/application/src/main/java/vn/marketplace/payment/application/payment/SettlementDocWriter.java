package vn.marketplace.payment.application.payment;

/**
 * Outbound port to the immutable (WORM) settlement-document store — S3 with Object Lock in
 * production. Write-once semantics: re-writing the same key with identical content returns the
 * existing URI (safe retry); attempting to overwrite with different content must be denied by the
 * store ({@code prevent overwrite/override}, TC-PAY-INT-03).
 */
public interface SettlementDocWriter {

    /** Persists the settlement document once and returns its immutable URI. */
    String writeOnce(String documentKey, String content);
}
