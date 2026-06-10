package vn.marketplace.payment.domain.payment.management;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Published when the payout instruction of a settlement is submitted to the bank. Notification
 * consumes it to inform the merchant ({@code payout_completed_merchant}). Carries only the last 4
 * digits of the bank account — the full number is L4 data and never leaves the aggregate.
 */
public class PayoutCompleted extends AbstractDomainEvent {

    private final String payoutId;
    private final String settlementId;
    private final String orderId;
    private final String merchantId;
    private final long amount;
    private final String bankLast4;

    public PayoutCompleted(Settlement settlement, DTime occurredAt) {
        super(DomainEventType.of("Payment", "PayoutCompleted"), occurredAt);
        Payout payout = settlement.payout();
        this.payoutId = payout.id().value();
        this.settlementId = settlement.id().value();
        this.orderId = settlement.orderId();
        this.merchantId = settlement.merchantId().value();
        this.amount = payout.amount().amount();
        String account = payout.bankAccount();
        this.bankLast4 = account.length() <= 4 ? account : account.substring(account.length() - 4);
        this.subject = this.payoutId;
    }

    public String payoutId() {
        return payoutId;
    }

    public String settlementId() {
        return settlementId;
    }

    public String orderId() {
        return orderId;
    }

    public String merchantId() {
        return merchantId;
    }

    public long amount() {
        return amount;
    }

    public String bankLast4() {
        return bankLast4;
    }
}
