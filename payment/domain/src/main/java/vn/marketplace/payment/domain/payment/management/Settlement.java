package vn.marketplace.payment.domain.payment.management;

import java.time.LocalDateTime;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.core.Snapshotable;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PayoutId;
import vn.marketplace.payment.domain.payment.SettlementId;
import vn.marketplace.payment.domain.shared.InvalidTransitionException;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;
import vn.marketplace.payment.domain.payment.SettlementStatus;

/**
 * Settlement aggregate root — the per-order reconciliation of one completed order: gross (released
 * escrow) − platform commission = net payout to the merchant.
 *
 * <p>Enforced invariants (tech-spec §2):
 * <ul>
 *   <li><b>WORM completeness</b> — {@link #complete()} requires {@code docUri != null}: no immutable
 *       settlement document, no completed settlement ({@code MISSING_SETTLEMENT_DOCUMENT}, TC-PAY-04).</li>
 *   <li><b>One-way lifecycle</b> {@code PROCESSING → COMPLETED}; completing twice is illegal.</li>
 *   <li><b>Arithmetic integrity</b> — {@code net = gross − commission}, computed once at creation via
 *       {@link CommissionPolicy} and immutable afterwards.</li>
 *   <li><b>Payout idempotency</b> — delegated to {@link Payout#submit()} (TC-PAY-06); the resulting
 *       {@code PayoutCompleted} is published exactly once.</li>
 * </ul>
 */
public class Settlement extends Aggregate<SettlementId> implements Snapshotable<Settlement.Memento> {

    private final String orderId;
    private final MerchantId merchantId;
    private final Money grossAmount;
    private final Money commission;
    private final Money netAmount;
    private SettlementStatus status;
    private String docUri;
    private final Payout payout;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    private Settlement(SettlementId id, String orderId, MerchantId merchantId, Money grossAmount,
                       Money commission, Money netAmount, Payout payout) {
        this.id = id;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.grossAmount = grossAmount;
        this.commission = commission;
        this.netAmount = netAmount;
        this.payout = payout;
        this.status = SettlementStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Reconstruction constructor — restores full state, raises no events. */
    private Settlement(Memento m, Payout payout) {
        this.id = new SettlementId(m.settlementId());
        this.set_id(m._id());
        this.orderId = m.orderId();
        this.merchantId = new MerchantId(m.merchantId());
        Currency currency = Currency.of(m.currency());
        this.grossAmount = Money.of(m.grossAmount(), currency);
        this.commission = Money.of(m.commission(), currency);
        this.netAmount = Money.of(m.netAmount(), currency);
        this.status = SettlementStatus.valueOf(m.status());
        this.docUri = m.docUri();
        this.payout = payout;
        this.createdAt = m.createdAt();
        this.completedAt = m.completedAt();
        this.updatedAt = m.updatedAt();
    }

    /**
     * Starts settling one completed order: applies the {@link CommissionPolicy} to the gross (released
     * escrow) and prepares the net payout instruction towards the merchant's bank account.
     */
    public static Settlement start(SettlementId id, String orderId, MerchantId merchantId,
                                   Money grossAmount, CommissionPolicy policy,
                                   PayoutId payoutId, String merchantBankAccount) {
        if (grossAmount.amount() <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.INVALID_AMOUNT);
        }
        Money commission = policy.commissionFor(grossAmount);
        Money net = grossAmount.minus(commission);
        Payout payout = new Payout(payoutId, merchantId, merchantBankAccount, net);
        return new Settlement(id, orderId, merchantId, grossAmount, commission, net, payout);
    }

    // ---- verb methods ----

    /** Records the WORM document URI returned by the immutable settlement-doc store. */
    public void attachDocument(String docUri) {
        requireProcessing("ATTACH_DOCUMENT");
        if (docUri == null || docUri.isBlank()) {
            throw new PaymentDomainException(PaymentErrorCode.MISSING_SETTLEMENT_DOCUMENT,
                    "A settlement document URI cannot be null or blank");
        }
        this.docUri = docUri;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * PROCESSING → COMPLETED. Refused while {@code docUri} is null — the WORM document is the legal
     * evidence of the settlement; without it the reconciliation never closes (TC-PAY-04).
     */
    public void complete() {
        requireProcessing("COMPLETE");
        if (this.docUri == null) {
            throw new PaymentDomainException(PaymentErrorCode.MISSING_SETTLEMENT_DOCUMENT);
        }
        this.status = SettlementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = this.completedAt;
    }

    /**
     * Submits the payout instruction (aggregate-root gate over {@link Payout#submit()}). First call
     * publishes {@code PayoutCompleted} and returns {@code true}; any later call is a silent no-op
     * returning {@code false} — never a duplicate bank instruction (TC-PAY-06).
     */
    public boolean submitPayout() {
        boolean submitted = payout.submit();
        if (submitted) {
            this.updatedAt = LocalDateTime.now();
            DomainEventPublisher.publish(new PayoutCompleted(this, DTime.now()));
        }
        return submitted;
    }

    private void requireProcessing(String event) {
        if (this.status != SettlementStatus.PROCESSING) {
            throw new InvalidTransitionException(this.status.name(), event);
        }
    }

    // ---- queries / accessors ----

    public String orderId() {
        return orderId;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public Money grossAmount() {
        return grossAmount;
    }

    public Money commission() {
        return commission;
    }

    public Money netAmount() {
        return netAmount;
    }

    public SettlementStatus status() {
        return status;
    }

    public String docUri() {
        return docUri;
    }

    public Payout payout() {
        return payout;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime completedAt() {
        return completedAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    // ---- Memento (persistence reconstruction, msfw Snapshotable) ----

    public static Settlement restore(Memento m) {
        return new Settlement(m, Payout.fromMemento(m.payout()));
    }

    @Override
    public Memento snapshot() {
        PayoutMemento payoutMemento = new PayoutMemento(payout.id().value(), payout.merchantId().value(),
                payout.bankAccount(), payout.amount().amount(), payout.amount().currency().name(),
                payout.status().name(), payout.submittedAt());
        return new Memento(_id(), id.value(), orderId, merchantId.value(), grossAmount.amount(),
                commission.amount(), netAmount.amount(), grossAmount.currency().name(), status.name(),
                docUri, createdAt, completedAt, updatedAt, payoutMemento);
    }

    public record Memento(Long _id,
                          String settlementId,
                          String orderId,
                          String merchantId,
                          long grossAmount,
                          long commission,
                          long netAmount,
                          String currency,
                          String status,
                          String docUri,
                          LocalDateTime createdAt,
                          LocalDateTime completedAt,
                          LocalDateTime updatedAt,
                          PayoutMemento payout) {
    }

    public record PayoutMemento(String payoutId, String merchantId, String bankAccount, long amount,
                                String currency, String status, LocalDateTime submittedAt) {
    }
}
