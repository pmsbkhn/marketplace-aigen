package vn.marketplace.payment.application.escrow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.eventsourcing.AggregateEventStore;
import tech.vsf.ptnt.msfw.domain.eventsourcing.EventSourcedRepository;
import tech.vsf.ptnt.msfw.domain.eventsourcing.SnapshotPolicy;
import tech.vsf.ptnt.msfw.domain.eventsourcing.SnapshotStore;
import tech.vsf.ptnt.msfw.domain.exception.ConcurrencyConflictException;

import vn.marketplace.payment.domain.escrow.EscrowLedger;
import vn.marketplace.payment.domain.escrow.EscrowLedgerId;

/**
 * The escrow bounded context as CQRS: each command updates the event-sourced write model and the
 * EscrowView read model consistently, both inside the Payment quantum.
 */
class EscrowCqrsTest {

    private final InMemoryEvents events = new InMemoryEvents();
    private final InMemorySnaps snaps = new InMemorySnaps();
    private final InMemoryViews views = new InMemoryViews();
    private EscrowLedgerService service;

    @BeforeEach
    void setUp() {
        DomainEventPublisher.clear();
        EventSourcedRepository<EscrowLedger, EscrowLedgerId, EscrowLedger.Memento> ledgers =
                new EventSourcedRepository<>(events, snaps, SnapshotPolicy.defaultPolicy()) {
                    @Override
                    protected EscrowLedger newInstance(EscrowLedgerId id) {
                        return new EscrowLedger(id);
                    }

                    @Override
                    protected EscrowLedger fromSnapshot(EscrowLedgerId id, EscrowLedger.Memento m) {
                        return new EscrowLedger(id, m.open(), m.held(), m.released());
                    }

                    @Override
                    protected String streamId(EscrowLedgerId id) {
                        return "escrow-" + id.value();
                    }
                };
        service = new EscrowLedgerService(ledgers, new EscrowProjector(views), views);
    }

    @Test
    void command_updates_write_model_and_read_model_consistently() {
        service.open("E-1");
        service.hold("E-1", 100);
        service.release("E-1", 40);

        EscrowView view = service.view("E-1").orElseThrow();   // query side
        assertTrue(view.open());
        assertEquals(60, view.held());
        assertEquals(40, view.released());

        // write side (event store) agrees — 3 events folded
        assertEquals(3, events.streams.get("escrow-E-1").size());
    }

    @Test
    void read_model_absent_until_a_command() {
        assertTrue(service.view("nope").isEmpty());
    }

    // ---- inline in-memory stores ----
    static final class InMemoryEvents implements AggregateEventStore {
        final Map<String, List<DomainEvent>> streams = new HashMap<>();

        public void append(String s, long expected, List<DomainEvent> e) {
            List<DomainEvent> st = streams.computeIfAbsent(s, k -> new ArrayList<>());
            if (st.size() != expected) {
                throw new ConcurrencyConflictException("v" + expected + " != " + st.size());
            }
            st.addAll(e);
        }

        public List<DomainEvent> load(String s) {
            return new ArrayList<>(streams.getOrDefault(s, List.of()));
        }

        public List<DomainEvent> loadAfter(String s, long after) {
            List<DomainEvent> st = streams.getOrDefault(s, List.of());
            return new ArrayList<>(st.subList((int) after, st.size()));
        }

        public long currentVersion(String s) {
            return streams.getOrDefault(s, List.of()).size();
        }
    }

    static final class InMemorySnaps implements SnapshotStore {
        final Map<String, Snapshot> m = new HashMap<>();

        public Optional<Snapshot> latest(String s) {
            return Optional.ofNullable(m.get(s));
        }

        public void save(String s, long v, Object memento) {
            m.put(s, new Snapshot(v, memento));
        }
    }

    static final class InMemoryViews implements EscrowViewStore {
        final Map<String, EscrowView> m = new HashMap<>();

        public Optional<EscrowView> find(String id) {
            return Optional.ofNullable(m.get(id));
        }

        public void save(EscrowView v) {
            m.put(v.escrowId(), v);
        }
    }
}
