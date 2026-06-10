package vn.marketplace.notification.standalone;

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
import vn.marketplace.notification.application.delivery.AcceptNotification;
import vn.marketplace.notification.application.delivery.AcceptNotificationUc;
import vn.marketplace.notification.application.delivery.DispatchNotification;
import vn.marketplace.notification.application.delivery.DispatchNotificationUc;
import vn.marketplace.notification.application.delivery.EncryptionPort;
import vn.marketplace.notification.application.delivery.GetNotification;
import vn.marketplace.notification.application.delivery.GetNotificationUc;
import vn.marketplace.notification.application.delivery.NotificationProviderPort;
import vn.marketplace.notification.application.delivery.PreferencePort;
import vn.marketplace.notification.domain.delivery.management.Notification;

/**
 * JSON transactional-outbox wiring for the standalone profile (no Kafka). Notification publishes no
 * domain events, but its state-writing use-cases are annotated {@code @EventPublishHandler}, so the
 * {@link EventPublishingProxyCreator} + JSON event-store beans must be present for the proxy to run.
 */
@Configuration
@Import(SpringCoreConfiguration.class)
@EnableJpaRepositories(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.notification.adapter.delivery.outbound.persistence"})
@EntityScan(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.notification.adapter.delivery.outbound.persistence.entity"})
public class NotificationStandaloneConfiguration {

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
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public AcceptNotification acceptNotification(Repository<Notification> repository, EncryptionPort encryptionPort) {
        return new AcceptNotificationUc(repository, encryptionPort);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public DispatchNotification dispatchNotification(Repository<Notification> repository,
                                                     PreferencePort preferencePort,
                                                     NotificationProviderPort providerPort) {
        return new DispatchNotificationUc(repository, preferencePort, providerPort);
    }

    @Bean
    public GetNotification getNotification(Repository<Notification> repository) {
        return new GetNotificationUc(repository);
    }
}
