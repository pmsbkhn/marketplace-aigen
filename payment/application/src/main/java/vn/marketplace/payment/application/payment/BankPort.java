package vn.marketplace.payment.application.payment;

/**
 * Outbound port to the merchant bank for payouts. The adapter ({@code BankClientOa}) handles the
 * bank API; called at most once per payout — the domain {@code Payout.submit()} no-op guard plus
 * the use-case ordering (bank call before state flip, both in one transaction) prevent duplicates.
 */
public interface BankPort {

    /** Submits one payout instruction. {@code bankAccount} is L4 data — never logged. */
    void submitPayout(String payoutId, String merchantId, String bankAccount, long amount, String currency);
}
