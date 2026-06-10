package vn.marketplace.payment.application.payment;

/**
 * Outbound port to the third-party payment gateway. The adapter ({@code GatewayClientOa}) speaks
 * HTTPS + HMAC with restricted egress; the application only sees this contract.
 */
public interface PaymentGatewayPort {

    /** Registers the escrow transaction with the gateway and returns the buyer redirect URL. */
    String createTransaction(String paymentId, String orderRef, long amount, String currency);
}
