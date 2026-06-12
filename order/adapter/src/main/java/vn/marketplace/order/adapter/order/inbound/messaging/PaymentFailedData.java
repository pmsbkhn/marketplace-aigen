package vn.marketplace.order.adapter.order.inbound.messaging;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Wire shape of Payment's {@code PaymentFailed} event payload (JSON serialization of the domain event). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedData(String paymentId, String orderRef, String reason, List<String> orderIds) {
}
