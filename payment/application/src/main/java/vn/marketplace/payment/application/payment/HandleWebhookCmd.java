package vn.marketplace.payment.application.payment;

/**
 * Verified webhook payload. {@code gatewayTxnId} is the gateway-side idempotency key (DB UNIQUE is
 * the final dedup authority); {@code amount} is cross-checked against the escrow snapshot by the
 * domain (AMOUNT_MISMATCH on drift). {@code status}: {@code SUCCESS} or anything else = failure.
 */
public record HandleWebhookCmd(String gatewayTxnId,
                               String orderRef,
                               long amount,
                               String status) {

    public static final String STATUS_SUCCESS = "SUCCESS";

    public boolean isSuccess() {
        return STATUS_SUCCESS.equalsIgnoreCase(status);
    }
}
