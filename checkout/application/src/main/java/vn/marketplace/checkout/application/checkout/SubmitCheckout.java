package vn.marketplace.checkout.application.checkout;

/**
 * Use case: run the whole checkout saga — authoritative pricing (Catalog) → atomic stock hold
 * (Inventory) → per-merchant pending orders (Order) → whole-cart escrow (Payment) → buyer redirect.
 * Idempotent by {@code idempotencyKey}; strict reverse-order compensation on any failure.
 */
public interface SubmitCheckout {

    CheckoutResultView execute(SubmitCheckoutCmd cmd);
}
