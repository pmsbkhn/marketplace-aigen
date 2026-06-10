package vn.marketplace.payment.application.payment;

import java.time.LocalDateTime;

/** Result of init-escrow: the escrow id (= payment id), buyer redirect URL and session expiry. */
public record InitEscrowResult(String escrowId, String paymentUrl, LocalDateTime expiresAt) {
}
