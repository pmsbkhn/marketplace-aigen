package vn.marketplace.payment.domain.escrow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

/**
 * The escrow ledger's state is the fold of its events. Verified with in-memory event/snapshot stores
 * (the JPA wiring is the same {@code EventSourcingStoreConfiguration} proven in msfw sample-service).
 */
class EscrowLedgerTest {

    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    private final InMemorySnapshots snapshotStore = new InMemorySnapshots();
    private EventSourcedRepository<EscrowLedger, EscrowLedgerId, EscrowLedger.Memento> escrows;

    @BeforeEach
    void setUp() {
        DomainEventPublisher.clear();
        escrows = new EventSourcedRepository<>(eventStore, snapshotStore, SnapshotPolicy.everyNEvents(3)) {
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
    }

    @Test
    void state_is_the_fold_of_the_stream() {
        EscrowLedgerId id = new EscrowLedgerId("E-1");
        EscrowLedger ledger = new EscrowLedger(id);
        ledger.open();
        ledger.hold(100);
        ledger.release(40);
        escrows.save(ledger);

        EscrowLedger loaded = escrows.load(id).orElseThrow();
        assertTrue(loaded.isOpen());
        assertEquals(60, loaded.held());
        assertEquals(40, loaded.released());
        assertEquals(3, loaded.version());
        assertEquals(3, DomainEventPublisher.getEvents().size());   // published for the outbox
    }

    @Test
    void optimistic_conflict_on_stale_version() {
        EscrowLedgerId id = new EscrowLedgerId("E-2");
        EscrowLedger seed = new EscrowLedger(id);
        seed.open();
        escrows.save(seed);

        EscrowLedger a1 = escrows.load(id).orElseThrow();
        EscrowLedger a2 = escrows.load(id).orElseThrow();
        a1.hold(10);
        escrows.save(a1);
        a2.hold(20);
        assertThrows(ConcurrencyConflictException.class, () -> escrows.save(a2));
    }

    // ---- inline in-memory stores ----
    static final class InMemoryEventStore implements AggregateEventStore {
        private final Map<String, List<DomainEvent>> streams = new HashMap<>();

        public void append(String streamId, long expectedVersion, List<DomainEvent> events) {
            List<DomainEvent> s = streams.computeIfAbsent(streamId, k -> new ArrayList<>());
            if (s.size() != expectedVersion) {
                throw new ConcurrencyConflictException("v" + expectedVersion + " != " + s.size());
            }
            s.addAll(events);
        }

        public List<DomainEvent> load(String streamId) {
            return new ArrayList<>(streams.getOrDefault(streamId, List.of()));
        }

        public List<DomainEvent> loadAfter(String streamId, long afterVersion) {
            List<DomainEvent> s = streams.getOrDefault(streamId, List.of());
            return new ArrayList<>(s.subList((int) afterVersion, s.size()));
        }

        public long currentVersion(String streamId) {
            return streams.getOrDefault(streamId, List.of()).size();
        }
    }

    static final class InMemorySnapshots implements SnapshotStore {
        private final Map<String, Snapshot> snaps = new HashMap<>();

        public Optional<Snapshot> latest(String streamId) {
            return Optional.ofNullable(snaps.get(streamId));
        }

        public void save(String streamId, long version, Object memento) {
            snaps.put(streamId, new Snapshot(version, memento));
        }
    }
}
