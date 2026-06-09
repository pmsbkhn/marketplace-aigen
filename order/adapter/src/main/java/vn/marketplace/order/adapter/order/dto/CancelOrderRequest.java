package vn.marketplace.order.adapter.order.dto;

/** Body for POST /v1/orders/{id}/cancel and the internal saga-compensation cancel. */
public record CancelOrderRequest(String reason) {
}
