package vn.marketplace.payment.adapter.payment.dto;

import java.util.List;

/**
 * The {@code OrderCompleted} event payload from OMS (Kafka in production, REST stand-in here) —
 * triggers settlement + payout for the completed order. Shape mirrors the Order context's
 * {@code OrderCompleted} domain event.
 */
public record OrderCompletedEvent(String eventId,
                                  String orderId,
                                  String merchantId,
                                  List<Line> items) {

    public record Line(String sku, int qty, long priceSnapshot) {
    }
}
