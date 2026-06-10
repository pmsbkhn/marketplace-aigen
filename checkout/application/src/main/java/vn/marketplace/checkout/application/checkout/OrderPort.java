package vn.marketplace.checkout.application.checkout;

import java.util.List;

/**
 * Outbound port to Order ({@code Order.CreatePendingOrder/CancelPendingOrder}). One call per
 * merchant group; {@code checkoutRef} must be unique per order (Order dedups on it), so the saga
 * derives it as {@code idempotencyKey + ":" + merchantId}.
 */
public interface OrderPort {

    String createPendingOrder(CreateOrderDto dto);

    /** Saga compensation — idempotent on Order's side (cancelling a CANCELLED order is a no-op). */
    void cancelOrder(String orderId, String reason);

    record CreateOrderDto(String checkoutRef,
                          String buyerId,
                          String merchantId,
                          ShippingAddressDto shippingAddress,
                          List<OrderLineDto> items) {
    }

    record ShippingAddressDto(String fullName, String phone, String addressLine,
                              String ward, String district, String city) {
    }

    record OrderLineDto(String productId, String variantId, String skuCode, String productName,
                        long priceSnapshot, String currency, int quantity) {
    }
}
