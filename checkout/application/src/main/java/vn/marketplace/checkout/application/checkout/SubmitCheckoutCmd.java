package vn.marketplace.checkout.application.checkout;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.IdempotencyKey;

/**
 * Submit-checkout command. Items carry NO price — prices come exclusively from Catalog at saga time
 * (Price Authority Invariant, TC-CHK-03). {@code idempotencyKey} arrives via the
 * {@code X-Idempotency-Key} header and identifies the saga, the stock reservation and the escrow;
 * the {@link IdempotencyKey} primitive rejects a blank header (non-blank guard → HTTP 400).
 */
public record SubmitCheckoutCmd(IdempotencyKey idempotencyKey,
                                String buyerId,
                                ShippingAddressInput address,
                                List<CheckoutItemInput> items) {

    public record CheckoutItemInput(String sku, int quantity, String merchantId) {
    }

    public record ShippingAddressInput(String fullName, String phone, String addressLine,
                                       String ward, String district, String city) {
    }
}
