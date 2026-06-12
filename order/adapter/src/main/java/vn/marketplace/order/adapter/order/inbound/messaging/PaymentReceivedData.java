package vn.marketplace.order.adapter.order.inbound.messaging;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire shape of Payment's {@code PaymentReceived} event payload (JSON serialization of the domain
 * event). One payment covers one checkout, which may span several orders — one per allocation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentReceivedData(String paymentId, String orderRef, List<Allocation> allocations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Allocation(String orderId, String merchantId, long amount) {
    }
}
