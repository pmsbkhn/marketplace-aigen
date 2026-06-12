package vn.marketplace.notification.adapter.delivery.inbound.messaging;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire shape of Payment's {@code PaymentReceived} event payload (JSON serialization of the domain
 * event). One allocation per paid order — each drives one merchant notification.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentReceivedData(String paymentId,
                                  String orderRef,
                                  long amount,
                                  List<Allocation> allocations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Allocation(String orderId, String merchantId, long amount) {
    }
}
