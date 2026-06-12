package vn.marketplace.order.adapter.order.inbound.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire shape of our own {@code Order/OrderPendingTimedOut} delayed event payload — the payment
 * deadline timer coming back through the outbox + Kafka at its {@code deliverAfter} instant.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPendingTimedOutData(String orderId) {
}
