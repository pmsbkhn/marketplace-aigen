package vn.marketplace.checkout.application.checkout;

import java.util.List;

/**
 * Outbound port to Payment ({@code Payment.InitEscrow}). ONE escrow for the whole cart (ADR-2),
 * allocated per order/merchant so Payment can settle each completed order independently.
 * {@code orderRef} (= the idempotency key) makes a saga retry return the same escrow.
 */
public interface PaymentPort {

    EscrowDto initEscrow(String orderRef, long totalAmount, String currency,
                         List<EscrowAllocationDto> allocations, String buyerId);

    record EscrowAllocationDto(String orderId, String merchantId, long amount) {
    }

    record EscrowDto(String escrowId, String paymentUrl) {
    }
}
