package vn.marketplace.payment.application.escrow;

import tech.vsf.ptnt.msfw.consumption.projection.Projector;
import tech.vsf.ptnt.msfw.domain.core.DomainEvent;

import vn.marketplace.payment.domain.escrow.EscrowOpened;
import vn.marketplace.payment.domain.escrow.FundsHeld;
import vn.marketplace.payment.domain.escrow.FundsReleased;

/**
 * CQRS query side: an idempotent {@link Projector} that folds the escrow event stream into the
 * {@link EscrowView} read model. Same events as the {@link vn.marketplace.payment.domain.escrow.EscrowLedger}
 * write model — command and query are two models over one quantum's data. Can run in-process
 * (read-your-writes) or be attached to a msfw projection pipeline for async/cross-context read models.
 */
public class EscrowProjector implements Projector<DomainEvent> {

    private final EscrowViewStore store;

    public EscrowProjector(EscrowViewStore store) {
        this.store = store;
    }

    @Override
    public void project(DomainEvent event) {
        String id = event.subject();
        EscrowView v = store.find(id).orElse(new EscrowView(id, false, 0, 0));
        EscrowView updated;
        if (event instanceof EscrowOpened) {
            updated = new EscrowView(id, true, v.held(), v.released());
        } else if (event instanceof FundsHeld held) {
            updated = new EscrowView(id, v.open(), v.held() + held.amount(), v.released());
        } else if (event instanceof FundsReleased released) {
            updated = new EscrowView(id, v.open(), v.held() - released.amount(),
                    v.released() + released.amount());
        } else {
            return;
        }
        store.save(updated);
    }
}
