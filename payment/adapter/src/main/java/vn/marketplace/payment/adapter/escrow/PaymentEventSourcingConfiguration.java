package vn.marketplace.payment.adapter.escrow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import tech.vsf.ptnt.msfw.domain.eventsourcing.AggregateEventStore;
import tech.vsf.ptnt.msfw.domain.eventsourcing.EventSourcedRepository;
import tech.vsf.ptnt.msfw.domain.eventsourcing.SnapshotPolicy;
import tech.vsf.ptnt.msfw.domain.eventsourcing.SnapshotStore;
import tech.vsf.ptnt.springcore.persistence.eventsourcing.EventSourcingStoreConfiguration;

import vn.marketplace.payment.domain.escrow.EscrowLedger;
import vn.marketplace.payment.domain.escrow.EscrowLedgerId;

/**
 * Opts the Payment service into event sourcing for the escrow ledger: imports the msfw JPA
 * event-store/snapshot-store beans ({@code aggregate_events}/{@code aggregate_snapshots}) and declares
 * the escrow repository over them. Picked up by the adapter component scan; the event store lives in
 * the Payment service's own database — inside the Payment architectural quantum.
 */
@Configuration
@Import(EventSourcingStoreConfiguration.class)
public class PaymentEventSourcingConfiguration {

    @Bean
    public EventSourcedRepository<EscrowLedger, EscrowLedgerId, EscrowLedger.Memento> escrowLedgerRepository(
            AggregateEventStore eventStore, SnapshotStore snapshotStore) {
        return new EventSourcedRepository<>(eventStore, snapshotStore, SnapshotPolicy.defaultPolicy()) {
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
}
