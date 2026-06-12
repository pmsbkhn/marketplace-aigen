package vn.marketplace.payment.adapter.payment.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The {@code OrderCompleted} event payload from OMS — triggers settlement + payout for the
 * completed order. Shape mirrors the Order context's {@code OrderCompleted} domain event; bound
 * straight from the Kafka CloudEvent's {@code data} node, so unknown fields are ignored.
 * {@code eventId} is not part of the payload (the envelope carries it) and stays null on the
 * event-driven path.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCompletedEvent(String eventId,
                                  String orderId,
                                  String merchantId,
                                  List<Line> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(String sku, int qty, long priceSnapshot) {
    }
}
