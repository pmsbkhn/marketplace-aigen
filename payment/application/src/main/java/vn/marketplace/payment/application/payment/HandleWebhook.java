package vn.marketplace.payment.application.payment;

/**
 * Use case: process a payment-gateway webhook that has ALREADY passed the adapter's HMAC-signature
 * and anti-replay verification (the trust boundary lives in {@code WebhookController}). Drives the
 * payment state machine and publishes {@code PaymentReceived}/{@code PaymentFailed} via the outbox.
 */
public interface HandleWebhook {

    void execute(HandleWebhookCmd cmd);
}
