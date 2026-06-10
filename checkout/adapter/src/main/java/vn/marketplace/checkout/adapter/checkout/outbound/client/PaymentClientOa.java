package vn.marketplace.checkout.adapter.checkout.outbound.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import vn.marketplace.checkout.application.checkout.PaymentPort;

/**
 * Outbound client to Payment ({@code Payment.InitEscrow}) — REST stand-in for the gRPC + mTLS
 * contract. One escrow per checkout, allocated per order/merchant.
 */
@Component
public class PaymentClientOa implements PaymentPort {

    private final RestClient rest;

    public PaymentClientOa(@Value("${checkout.clients.payment-url:http://localhost:8083}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public EscrowDto initEscrow(String orderRef, long totalAmount, String currency,
                                List<EscrowAllocationDto> allocations, String buyerId) {
        List<AllocationBody> body = allocations.stream()
                .map(a -> new AllocationBody(a.orderId(), a.merchantId(), a.amount()))
                .toList();
        Envelope<EscrowBody> envelope = rest.post()
                .uri("/internal/payments/escrow")
                .body(new InitEscrowBody(orderRef, totalAmount, currency, body))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return new EscrowDto(envelope.data().escrowId(), envelope.data().paymentUrl());
    }

    record InitEscrowBody(String orderRef, long amount, String currency, List<AllocationBody> allocations) {
    }

    record AllocationBody(String orderId, String merchantId, long amount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EscrowBody(String escrowId, String paymentUrl, String expiresAt) {
    }
}
