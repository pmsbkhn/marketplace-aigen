package vn.marketplace.payment.adapter.escrow;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.domain.eventsourcing.AggregateEventStore;
import tech.vsf.ptnt.msfw.domain.eventsourcing.EventSourcedRepository;
import tech.vsf.ptnt.msfw.domain.eventsourcing.SnapshotPolicy;
import tech.vsf.ptnt.msfw.domain.eventsourcing.SnapshotStore;
import tech.vsf.ptnt.springcore.persistence.eventsourcing.EventSourcingStoreConfiguration;

import vn.marketplace.payment.application.escrow.EscrowLedgerService;
import vn.marketplace.payment.application.escrow.EscrowProjector;
import vn.marketplace.payment.application.escrow.EscrowViewStore;
import vn.marketplace.payment.domain.escrow.EscrowLedger;
import vn.marketplace.payment.domain.escrow.EscrowLedgerId;

/**
 * Wires the escrow CQRS bounded context in the Payment service: the event-sourced write model
 * ({@link EscrowLedger} over the msfw JPA event store) and the read model (the {@link EscrowProjector}
 * folding events into the {@code escrow_view} table via {@link EscrowViewStore}). Command + query +
 * event store all in the Payment database — one architectural quantum. Boot's {@code @EntityScan}/
 * {@code @EnableJpaRepositories} accumulate across configs, so this adds the escrow read-model
 * entity/repository alongside the msfw event-store ones (imported) and the existing payment ones.
 */
@Configuration
@Import(EventSourcingStoreConfiguration.class)
@EntityScan(basePackages = "vn.marketplace.payment.adapter.escrow")
@EnableJpaRepositories(basePackages = "vn.marketplace.payment.adapter.escrow")
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

    @Bean
    public EscrowProjector escrowProjector(EscrowViewStore viewStore) {
        return new EscrowProjector(viewStore);
    }

    @Bean
    public EscrowLedgerService escrowLedgerService(
            EventSourcedRepository<EscrowLedger, EscrowLedgerId, EscrowLedger.Memento> escrowLedgerRepository,
            EscrowProjector escrowProjector, EscrowViewStore viewStore) {
        return new EscrowLedgerService(escrowLedgerRepository, escrowProjector, viewStore);
    }
}
