package vn.marketplace.payment.domain.payment.management;

import java.time.LocalDateTime;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.EscrowHoldId;
import vn.marketplace.payment.domain.payment.EscrowStatus;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * One per-order escrow allocation inside the {@link Payment} aggregate boundary. Lifecycle is one-way
 * {@code HELD → RELEASED} and terminal-frozen; the PAID guard lives on the aggregate root
 * ({@link Payment#releaseForOrder}), never here — a hold cannot release itself out from under an
 * unpaid payment.
 */
public class EscrowHold implements Entity {

    private final EscrowHoldId id;
    private final String orderId;
    private final MerchantId merchantId;
    private final Money amount;
    private EscrowStatus status;
    private LocalDateTime releasedAt;

    EscrowHold(EscrowHoldId id, String orderId, MerchantId merchantId, Money amount) {
        if (amount.amount() <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.INVALID_AMOUNT);
        }
        this.id = id;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = EscrowStatus.HELD;
    }

    private EscrowHold(EscrowHoldId id, String orderId, MerchantId merchantId, Money amount,
                       EscrowStatus status, LocalDateTime releasedAt) {
        this.id = id;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.releasedAt = releasedAt;
    }

    static EscrowHold fromMemento(Payment.HoldMemento m) {
        return new EscrowHold(new EscrowHoldId(m.holdId()), m.orderId(), new MerchantId(m.merchantId()),
                Money.of(m.amount(), Currency.of(m.currency())), EscrowStatus.valueOf(m.status()),
                m.releasedAt());
    }

    /** Package-private: only the aggregate root may release (after its PAID guard). Idempotent. */
    boolean release() {
        if (status == EscrowStatus.RELEASED) {
            return false; // already released — no-op
        }
        this.status = EscrowStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        return true;
    }

    public EscrowHoldId id() {
        return id;
    }

    public String orderId() {
        return orderId;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public Money amount() {
        return amount;
    }

    public EscrowStatus status() {
        return status;
    }

    public LocalDateTime releasedAt() {
        return releasedAt;
    }
}
