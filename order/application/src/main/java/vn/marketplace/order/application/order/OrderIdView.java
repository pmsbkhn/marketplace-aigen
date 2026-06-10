package vn.marketplace.order.application.order;

/** Result of creating an order. */
public record OrderIdView(String orderId, String status, long totalAmount) {
}
