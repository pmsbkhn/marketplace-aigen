package vn.marketplace.payment.domain.escrow;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

public class EscrowOpened extends AbstractDomainEvent {

    public EscrowOpened(EscrowLedgerId escrowId, DTime occurredAt) {
        super(DomainEventType.of("Payment", "EscrowOpened"), occurredAt);
        this.subject = escrowId.value();
    }
}
