package vn.marketplace.payment.application.payment;

/**
 * Use case: submit the net payout of a completed settlement to the merchant bank. Idempotent — the
 * {@code Payout.submit()} no-op guard means a retry never sends a second bank instruction
 * (TC-PAY-06). Publishes {@code PayoutCompleted} exactly once.
 */
public interface ProcessPayout {

    SettlementView execute(ProcessPayoutCmd cmd);
}
