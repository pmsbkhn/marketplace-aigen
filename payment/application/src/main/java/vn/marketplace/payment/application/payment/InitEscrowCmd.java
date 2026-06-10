package vn.marketplace.payment.application.payment;

import java.util.List;

/**
 * Init-escrow command (from Checkout). {@code orderRef} is the idempotency key (no duplicate escrow
 * for the same checkout); {@code allocations} must sum exactly to {@code amount} — one escrow for
 * the whole cart, allocated per order/merchant for later settlement.
 */
public record InitEscrowCmd(String orderRef,
                            long amount,
                            String currency,
                            List<EscrowAllocationInput> allocations) {

    public record EscrowAllocationInput(String orderId, String merchantId, long amount) {
    }
}
