package vn.marketplace.payment.adapter.payment.dto;

import java.util.List;

/** Init-escrow request from the Checkout orchestrator (gRPC stand-in, see InternalPaymentController). */
public record InitEscrowRequest(String orderRef,
                                long amount,
                                String currency,
                                List<Allocation> allocations) {

    public record Allocation(String orderId, String merchantId, long amount) {
    }
}
