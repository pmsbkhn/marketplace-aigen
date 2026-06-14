package vn.marketplace.payment.application.escrow;

import java.util.List;
import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.core.DomainEvent;
import tech.vsf.ptnt.msfw.domain.eventsourcing.EventSourcedRepository;

import vn.marketplace.payment.domain.escrow.EscrowLedger;
import vn.marketplace.payment.domain.escrow.EscrowLedgerId;

/**
 * Escrow application service tying the CQRS halves together in one quantum: each command mutates the
 * event-sourced {@link EscrowLedger} (write model) and projects the new events into the
 * {@link EscrowView} read model (query side), in-process — read-your-writes within the Payment quantum.
 */
public class EscrowLedgerService {

    private final EventSourcedRepository<EscrowLedger, EscrowLedgerId, EscrowLedger.Memento> ledgers;
    private final EscrowProjector projector;
    private final EscrowViewStore views;

    public EscrowLedgerService(EventSourcedRepository<EscrowLedger, EscrowLedgerId, EscrowLedger.Memento> ledgers,
                               EscrowProjector projector, EscrowViewStore views) {
        this.ledgers = ledgers;
        this.projector = projector;
        this.views = views;
    }

    public void open(String escrowId) {
        EscrowLedger ledger = new EscrowLedger(new EscrowLedgerId(escrowId));
        ledger.open();
        persist(ledger);
    }

    public void hold(String escrowId, long amount) {
        EscrowLedger ledger = ledgers.load(new EscrowLedgerId(escrowId)).orElseThrow();
        ledger.hold(amount);
        persist(ledger);
    }

    public void release(String escrowId, long amount) {
        EscrowLedger ledger = ledgers.load(new EscrowLedgerId(escrowId)).orElseThrow();
        ledger.release(amount);
        persist(ledger);
    }

    /** Query side. */
    public Optional<EscrowView> view(String escrowId) {
        return views.find(escrowId);
    }

    private void persist(EscrowLedger ledger) {
        List<DomainEvent> newEvents = ledger.uncommittedEvents();   // capture before save clears them
        ledgers.save(ledger);                                       // write model (event store)
        newEvents.forEach(projector::project);                      // read model (query side)
    }
}
