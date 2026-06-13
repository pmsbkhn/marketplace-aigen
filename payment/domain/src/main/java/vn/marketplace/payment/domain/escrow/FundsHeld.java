package vn.marketplace.payment.domain.escrow;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

public class FundsHeld extends AbstractDomainEvent {

    private final long amount;

    public FundsHeld(EscrowLedgerId escrowId, long amount, DTime occurredAt) {
        super(DomainEventType.of("Payment", "FundsHeld"), occurredAt);
        this.subject = escrowId.value();
        this.amount = amount;
    }

    public long amount() {
        return amount;
    }
}
