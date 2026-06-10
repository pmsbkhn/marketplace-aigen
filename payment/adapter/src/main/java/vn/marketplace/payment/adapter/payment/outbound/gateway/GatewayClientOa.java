package vn.marketplace.payment.adapter.payment.outbound.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import vn.marketplace.payment.application.payment.PaymentGatewayPort;

/**
 * Outbound adapter to the third-party payment gateway. In production this is an HTTPS + HMAC client
 * inside the restricted-egress subnet (egress only to the gateway/bank); on the standalone profile
 * it deterministically builds the redirect URL so the whole flow is exercisable with no real PG.
 */
@Component
public class GatewayClientOa implements PaymentGatewayPort {

    private final String gatewayBaseUrl;

    public GatewayClientOa(@Value("${payment.gateway.base-url:https://pg.example.com}") String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    @Override
    public String createTransaction(String paymentId, String orderRef, long amount, String currency) {
        return gatewayBaseUrl + "/pay/" + paymentId + "?ref=" + orderRef;
    }
}
