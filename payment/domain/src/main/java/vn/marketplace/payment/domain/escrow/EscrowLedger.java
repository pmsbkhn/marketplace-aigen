package vn.marketplace.payment.domain.escrow;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

import tech.vsf.ptnt.msfw.domain.core.Snapshotable;
import tech.vsf.ptnt.msfw.domain.eventsourcing.EventSourcedAggregate;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Event-sourced escrow ledger: an audit-native record of money held/released during checkout, kept
 * as a stream of events (msfw Event Sourcing, ADR 0001) — opt-in alongside the state-stored escrow
 * model. Command side + the (future) read side + this event stream all live in the Payment service:
 * one architectural quantum.
 */
public class EscrowLedger extends EventSourcedAggregate<EscrowLedgerId>
        implements Snapshotable<EscrowLedger.Memento> {

    private boolean open;
    private long held;
    private long released;

    public EscrowLedger(EscrowLedgerId id) {
        this.id = id;
    }

    public EscrowLedger(EscrowLedgerId id, boolean open, long held, long released) {
        this.id = id;
        this.open = open;
        this.held = held;
        this.released = released;
    }

    // ---- commands ----
    public void open() {
        if (open) {
            throw new IllegalStateException("escrow already open");
        }
        apply(new EscrowOpened(id, DTime.now()));
    }

    public void hold(long amount) {
        if (!open) {
            throw new IllegalStateException("escrow not open");
        }
        if (amount <= 0) {
            throw new InvalidArgumentException("amount must be positive");
        }
        apply(new FundsHeld(id, amount, DTime.now()));
    }

    public void release(long amount) {
        if (amount <= 0 || amount > held) {
            throw new InvalidArgumentException("cannot release " + amount + " of " + held + " held");
        }
        apply(new FundsReleased(id, amount, DTime.now()));
    }

    // ---- state transitions (the fold) ----
    public void on(EscrowOpened event) {
        this.open = true;
    }

    public void on(FundsHeld event) {
        this.held += event.amount();
    }

    public void on(FundsReleased event) {
        this.held -= event.amount();
        this.released += event.amount();
    }

    public boolean isOpen() {
        return open;
    }

    public long held() {
        return held;
    }

    public long released() {
        return released;
    }

    @Override
    public Memento snapshot() {
        return new Memento(id.value(), open, held, released);
    }

    public record Memento(String escrowId, boolean open, long held, long released) {
    }
}
