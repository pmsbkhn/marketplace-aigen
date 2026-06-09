package vn.marketplace.order.adapter.order.dto;

import java.util.List;

/**
 * Body for the internal create-order endpoint (stand-in for gRPC {@code Order.CreatePendingOrder}
 * called by Checkout). One request = one merchant's order (Checkout splits the cart).
 */
public record CreateOrderRequest(String checkoutRef,
                                 String buyerId,
                                 String merchantId,
                                 Address shippingAddress,
                                 List<Item> items) {

    public record Address(String fullName, String phone, String addressLine,
                          String ward, String district, String city) {
    }

    public record Item(String productId, String variantId, String skuCode, String productName,
                       long priceSnapshot, String currency, int quantity) {
    }
}
