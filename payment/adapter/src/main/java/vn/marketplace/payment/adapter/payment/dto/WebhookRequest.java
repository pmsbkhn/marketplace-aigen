package vn.marketplace.payment.adapter.payment.dto;

/**
 * Payment-gateway webhook payload. {@code timestamp} (epoch seconds) is part of the signed body and
 * is checked against the anti-replay window; {@code gatewayTxnId} is the gateway-side idempotency
 * key (DB UNIQUE is the final dedup authority).
 */
public record WebhookRequest(String gatewayTxnId,
                             String orderRef,
                             long amount,
                             String status,
                             long timestamp) {
}
