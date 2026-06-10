package vn.marketplace.checkout.adapter.checkout.dto;

import java.util.List;

/**
 * Checkout submit body. NO price field exists anywhere in this contract — prices are snapshotted
 * server-side from Catalog (Price Authority Invariant). Identity comes from gateway headers, the
 * idempotency key from {@code X-Idempotency-Key}.
 */
public record CheckoutRequest(Address shippingAddress, List<Item> items) {

    public record Item(String sku, int quantity, String merchantId) {
    }

    public record Address(String fullName, String phone, String addressLine,
                          String ward, String district, String city) {
    }
}
