package vn.marketplace.payment.application.payment;

import java.util.List;

/**
 * Settle-order command (from the {@code OrderCompleted} event). {@code orderId} is the idempotency
 * key. {@code items} (price snapshots from the order) cross-check the escrow hold amount — defense
 * in depth. {@code merchantBankAccount} is resolved by the adapter from the merchant directory
 * (Identity context stand-in) — L4 data, never logged.
 */
public record ProcessSettlementCmd(String orderId,
                                   String merchantId,
                                   List<SettlementItemInput> items,
                                   String merchantBankAccount) {

    public record SettlementItemInput(String skuCode, long priceSnapshot, int quantity) {
    }
}
