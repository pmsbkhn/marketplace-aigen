package vn.marketplace.payment.domain.payment.management;

import java.time.LocalDateTime;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PayoutId;
import vn.marketplace.payment.domain.payment.PayoutStatus;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * The bank-transfer instruction inside the {@link Settlement} aggregate boundary. {@link #submit()}
 * is idempotent: once SUBMITTED, calling it again is a no-op returning {@code false} — a duplicate
 * instruction would send real money to the merchant twice (TC-PAY-06). The bank account is L4 data:
 * persisted field-level encrypted by the adapter, never logged.
 */
public class Payout implements Entity {

    private final PayoutId id;
    private final MerchantId merchantId;
    private final String bankAccount;
    private final Money amount;
    private PayoutStatus status;
    private LocalDateTime submittedAt;

    Payout(PayoutId id, MerchantId merchantId, String bankAccount, Money amount) {
        if (bankAccount == null || bankAccount.isBlank()) {
            throw new PaymentDomainException(PaymentErrorCode.BANK_ACCOUNT_REQUIRED);
        }
        if (amount.amount() <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.INVALID_AMOUNT);
        }
        this.id = id;
        this.merchantId = merchantId;
        this.bankAccount = bankAccount;
        this.amount = amount;
        this.status = PayoutStatus.PENDING;
    }

    private Payout(PayoutId id, MerchantId merchantId, String bankAccount, Money amount,
                   PayoutStatus status, LocalDateTime submittedAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.bankAccount = bankAccount;
        this.amount = amount;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    static Payout fromMemento(Settlement.PayoutMemento m) {
        return new Payout(new PayoutId(m.payoutId()), new MerchantId(m.merchantId()), m.bankAccount(),
                Money.of(m.amount(), Currency.of(m.currency())), PayoutStatus.valueOf(m.status()),
                m.submittedAt());
    }

    /**
     * Marks the instruction as submitted to the bank. Returns {@code true} on the first call,
     * {@code false} (no-op) on any later call — never a double bank instruction (TC-PAY-06).
     */
    public boolean submit() {
        if (status == PayoutStatus.SUBMITTED) {
            return false; // already submitted — block the duplicate
        }
        this.status = PayoutStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        return true;
    }

    public PayoutId id() {
        return id;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public String bankAccount() {
        return bankAccount;
    }

    public Money amount() {
        return amount;
    }

    public PayoutStatus status() {
        return status;
    }

    public LocalDateTime submittedAt() {
        return submittedAt;
    }
}
