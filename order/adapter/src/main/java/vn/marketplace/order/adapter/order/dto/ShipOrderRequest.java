package vn.marketplace.order.adapter.order.dto;

/** Body for POST /v1/merchant/orders/{id}/ship. */
public record ShipOrderRequest(String trackingNumber) {
}
