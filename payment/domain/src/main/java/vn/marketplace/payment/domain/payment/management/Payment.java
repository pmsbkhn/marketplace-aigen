package vn.marketplace.payment.domain.payment.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.EscrowHoldId;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.PaymentStatus;
import vn.marketplace.payment.domain.shared.InvalidTransitionException;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * Payment aggregate root — the real-money source of truth for one checkout (one escrow for the whole
 * cart, allocated per order/merchant as {@link EscrowHold}s).
 *
 * <p>Enforced invariants (tech-spec §2):
 * <ol>
 *   <li><b>One-way state machine</b> {@code PENDING → (PAID | FAILED)}; terminal states frozen —
 *       any further transition throws {@link InvalidTransitionException} (TC-PAY-02).</li>
 *   <li><b>Escrow release guard</b> — {@link #releaseForOrder} is legal only when this payment is
 *       PAID; otherwise {@code PAYMENT_NOT_COMPLETED} (TC-PAY-03).</li>
 *   <li><b>Amount cross-check</b> — the webhook amount must equal the {@code amount} snapshot taken
 *       at escrow init; any drift throws {@code AMOUNT_MISMATCH} and the payment stays PENDING
 *       (TC-PAY-01, anti webhook-tampering).</li>
 *   <li><b>Allocation integrity</b> — allocations are non-empty and sum exactly to the total.</li>
 * </ol>
 * {@code PaymentReceived}/{@code PaymentFailed} are published exactly on the matching transition.
 */
public class Payment extends Aggregate<PaymentId> {

    private final String orderRef;
    private final Money amount;
    private PaymentStatus status;
    private final List<EscrowHold> holds;
    private String gatewayTxnId;
    private String paymentUrl;
    private String failReason;
    private final LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime updatedAt;

    private Payment(PaymentId id, String orderRef, Money amount, List<EscrowHold> holds) {
        if (amount.amount() <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.INVALID_AMOUNT);
        }
        if (holds.isEmpty()) {
            throw new PaymentDomainException(PaymentErrorCode.EMPTY_ALLOCATIONS);
        }
        long allocated = holds.stream().mapToLong(h -> h.amount().amount()).sum();
        if (allocated != amount.amount()) {
            throw new PaymentDomainException(PaymentErrorCode.ALLOCATION_SUM_MISMATCH,
                    "Allocations sum to " + allocated + " but escrow total is " + amount.amount());
        }
        this.id = id;
        this.orderRef = orderRef;
        this.amount = amount;
        this.holds = new ArrayList<>(holds);
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Reconstruction constructor — restores full state, raises no events. */
    private Payment(Memento m, List<EscrowHold> holds) {
        this.id = new PaymentId(m.paymentId());
        this.set_id(m._id());
        this.orderRef = m.orderRef();
        this.amount = Money.of(m.amount(), Currency.of(m.currency()));
        this.status = PaymentStatus.valueOf(m.status());
        this.holds = new ArrayList<>(holds);
        this.gatewayTxnId = m.gatewayTxnId();
        this.paymentUrl = m.paymentUrl();
        this.failReason = m.failReason();
        this.createdAt = m.createdAt();
        this.paidAt = m.paidAt();
        this.updatedAt = m.updatedAt();
    }

    /** Init escrow for a whole cart (called by Checkout): one PENDING payment, one hold per order. */
    public static Payment initEscrow(PaymentId id, String orderRef, Money amount,
                                     List<NewAllocation> allocations) {
        List<EscrowHold> holds = new ArrayList<>();
        if (allocations != null) {
            for (NewAllocation a : allocations) {
                holds.add(new EscrowHold(new EscrowHoldId(a.holdId()), a.orderId(),
                        new MerchantId(a.merchantId()), Money.of(a.amount(), amount.currency())));
            }
        }
        return new Payment(id, orderRef, amount, holds);
    }

    // ---- verb methods (state machine — the only way to change status) ----

    /**
     * PENDING → PAID on a verified gateway webhook. The webhook amount must match the init-escrow
     * snapshot exactly — mismatch throws {@code AMOUNT_MISMATCH} <em>before</em> any state change, so
     * the payment stays PENDING (TC-PAY-01). Publishes {@code PaymentReceived}.
     */
    public void confirmPayment(String gatewayTxnId, long webhookAmount) {
        requirePending("CONFIRM_PAYMENT");
        if (webhookAmount != this.amount.amount()) {
            throw new PaymentDomainException(PaymentErrorCode.AMOUNT_MISMATCH,
                    "Webhook amount " + webhookAmount + " does not match escrow amount " + this.amount.amount());
        }
        this.gatewayTxnId = gatewayTxnId;
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = this.paidAt;
        DomainEventPublisher.publish(new PaymentReceived(this, DTime.now()));
    }

    /** PENDING → FAILED (gateway reported failure/timeout). Publishes {@code PaymentFailed}. */
    public void markFailed(String reason) {
        requirePending("MARK_FAILED");
        this.failReason = reason;
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
        DomainEventPublisher.publish(new PaymentFailed(this, reason, DTime.now()));
    }

    /**
     * Releases the escrow hold of one completed order. Guard: the payment must be PAID — releasing
     * buyer money that never arrived would leak platform funds (TC-PAY-03). Idempotent per hold.
     *
     * @return the released hold (carrying the gross amount for settlement)
     */
    public EscrowHold releaseForOrder(String orderId) {
        EscrowHold hold = holds.stream()
                .filter(h -> h.orderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new PaymentDomainException(PaymentErrorCode.ESCROW_HOLD_NOT_FOUND,
                        "No escrow hold for order " + orderId));
        if (this.status != PaymentStatus.PAID) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_COMPLETED,
                    "Cannot release escrow for order " + orderId + " while payment is " + this.status);
        }
        if (hold.release()) {
            this.updatedAt = LocalDateTime.now();
        }
        return hold;
    }

    /** Records the redirect URL returned by the payment gateway at escrow init. */
    public void attachPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
        this.updatedAt = LocalDateTime.now();
    }

    private void requirePending(String event) {
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidTransitionException(this.status.name(), event);
        }
    }

    // ---- queries / accessors ----

    public String orderRef() {
        return orderRef;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }

    public List<EscrowHold> holds() {
        return List.copyOf(holds);
    }

    public String gatewayTxnId() {
        return gatewayTxnId;
    }

    public String paymentUrl() {
        return paymentUrl;
    }

    public String failReason() {
        return failReason;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime paidAt() {
        return paidAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /** Input spec for one per-order allocation at escrow init. */
    public record NewAllocation(String holdId, String orderId, String merchantId, long amount) {
    }

    // ---- Memento (persistence reconstruction) ----

    public static Payment fromMemento(Memento m) {
        List<EscrowHold> holds = m.holds().stream().map(EscrowHold::fromMemento).toList();
        return new Payment(m, holds);
    }

    public Memento toMemento() {
        List<HoldMemento> holdMementos = new ArrayList<>();
        for (EscrowHold h : holds) {
            holdMementos.add(new HoldMemento(h.id().value(), h.orderId(), h.merchantId().value(),
                    h.amount().amount(), h.amount().currency().name(), h.status().name(), h.releasedAt()));
        }
        return new Memento(_id(), id.value(), orderRef, amount.amount(), amount.currency().name(),
                status.name(), gatewayTxnId, paymentUrl, failReason, createdAt, paidAt, updatedAt,
                holdMementos);
    }

    public record Memento(Long _id,
                          String paymentId,
                          String orderRef,
                          long amount,
                          String currency,
                          String status,
                          String gatewayTxnId,
                          String paymentUrl,
                          String failReason,
                          LocalDateTime createdAt,
                          LocalDateTime paidAt,
                          LocalDateTime updatedAt,
                          List<HoldMemento> holds) {
    }

    public record HoldMemento(String holdId, String orderId, String merchantId, long amount,
                              String currency, String status, LocalDateTime releasedAt) {
    }
}
