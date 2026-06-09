package vn.marketplace.order.application.order;

import java.util.List;

/**
 * Create-order command (from Checkout). {@code checkoutRef} is the idempotency key (a duplicate
 * returns the existing order). Prices are snapshots forwarded from Catalog via Checkout — the order
 * never re-queries Catalog.
 */
public record CreatePendingOrderCmd(String checkoutRef,
                                    String buyerId,
                                    String merchantId,
                                    ShippingAddressInput shippingAddress,
                                    List<OrderItemInput> items) {

    public record ShippingAddressInput(String fullName, String phone, String addressLine,
                                       String ward, String district, String city) {
    }

    public record OrderItemInput(String productId, String variantId, String skuCode, String productName,
                                 long priceSnapshot, String currency, int quantity) {
    }
}
