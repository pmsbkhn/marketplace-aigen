package vn.marketplace.payment.standalone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.EventPublishingProxyCreator;
import tech.vsf.ptnt.msfw.domain.DomainRegistry;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.DomainEventProcessor;
import tech.vsf.ptnt.msfw.event.handling.EventCoreConstants;
import tech.vsf.ptnt.msfw.event.handling.EventProcessorManager;
import tech.vsf.ptnt.msfw.event.handling.EventProcessorManagerWrapper;
import tech.vsf.ptnt.msfw.infrastructure.JpaEventStore;
import tech.vsf.ptnt.msfw.infrastructure.persistence.OutboxEventRepository;
import tech.vsf.ptnt.msfw.outbox.store.DataSerializationProcessor;
import tech.vsf.ptnt.msfw.outbox.store.EventStore;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import tech.vsf.ptnt.msfw.outbox.store.OutboxEventConverter;
import tech.vsf.ptnt.msfw.store.JsonSerializationProcessor;
import tech.vsf.ptnt.springcore.configuration.SpringCoreConfiguration;
import vn.marketplace.payment.application.payment.BankPort;
import vn.marketplace.payment.application.payment.GetPayment;
import vn.marketplace.payment.application.payment.GetPaymentUc;
import vn.marketplace.payment.application.payment.HandleWebhook;
import vn.marketplace.payment.application.payment.HandleWebhookUc;
import vn.marketplace.payment.application.payment.InitEscrow;
import vn.marketplace.payment.application.payment.InitEscrowUc;
import vn.marketplace.payment.application.payment.PaymentGatewayPort;
import vn.marketplace.payment.application.payment.PaymentRepository;
import vn.marketplace.payment.application.payment.ProcessPayout;
import vn.marketplace.payment.application.payment.ProcessPayoutUc;
import vn.marketplace.payment.application.payment.ProcessSettlement;
import vn.marketplace.payment.application.payment.ProcessSettlementUc;
import vn.marketplace.payment.application.payment.SettlementDocWriter;
import vn.marketplace.payment.domain.payment.management.CommissionPolicy;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.payment.management.Settlement;

/**
 * JSON transactional-outbox wiring for the standalone profile (no Kafka) — mirrors msfw's
 * sample-service outbox configuration. Payment publishes {@code PaymentReceived} /
 * {@code PaymentFailed} / {@code PayoutCompleted}; the {@link EventPublishingProxyCreator} drains
 * them to {@link JsonEventStoreProcessor} (H2 outbox).
 */
@Configuration
@Import(SpringCoreConfiguration.class)
@EnableJpaRepositories(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.payment.adapter.payment.outbound.persistence"})
@EntityScan(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.payment.adapter.payment.outbound.persistence.entity"})
public class PaymentStandaloneConfiguration {

    @Bean
    public EventStore eventStore(OutboxEventRepository repository) {
        return new JpaEventStore(repository);
    }

    @Bean("eventProcessorManager")
    @DependsOn("domainRegistry")
    public EventProcessorManager eventProcessorManager(DomainRegistry registry) {
        EventProcessorManagerWrapper manager = new EventProcessorManagerWrapper();
        registry.registerComponent(EventCoreConstants.EVENT_PROCESSOR_MANAGER, manager);
        return manager;
    }

    @Bean
    public DataSerializationProcessor<String> jsonDataSerializationProcessor() {
        return new JsonSerializationProcessor();
    }

    @Bean
    public OutboxEventConverter<String> jsonOutboxEventConverter(DataSerializationProcessor<String> jsonSerializer) {
        return new OutboxEventConverter<>(jsonSerializer);
    }

    @Bean("jsonEventStoreProcessor")
    @DependsOn("eventProcessorManager")
    public DomainEventProcessor jsonEventStoreProcessor(EventStore eventStore,
                                                        EventProcessorManager manager,
                                                        OutboxEventConverter<String> converter) {
        DomainEventProcessor processor = new JsonEventStoreProcessor(eventStore, converter);
        manager.addEventProcessor(processor);
        return processor;
    }

    @Bean
    public EventPublishingProxyCreator eventPublishingProxyCreator() {
        return new EventPublishingProxyCreator();
    }

    @Bean
    public CommissionPolicy commissionPolicy(
            @Value("${payment.commission.rate-basis-points:200}") int rateBasisPoints) {
        return new CommissionPolicy(rateBasisPoints);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public InitEscrow initEscrow(Repository<Payment> paymentRepository, PaymentGatewayPort gateway) {
        return new InitEscrowUc(paymentRepository, gateway);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public HandleWebhook handleWebhook(Repository<Payment> paymentRepository) {
        return new HandleWebhookUc(paymentRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ProcessSettlement processSettlement(PaymentRepository paymentRepository,
                                               Repository<Settlement> settlementRepository,
                                               SettlementDocWriter settlementDocWriter,
                                               CommissionPolicy commissionPolicy) {
        return new ProcessSettlementUc(paymentRepository, settlementRepository, settlementDocWriter,
                commissionPolicy);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ProcessPayout processPayout(Repository<Settlement> settlementRepository, BankPort bank) {
        return new ProcessPayoutUc(settlementRepository, bank);
    }

    @Bean
    public GetPayment getPayment(Repository<Payment> paymentRepository) {
        return new GetPaymentUc(paymentRepository);
    }
}
