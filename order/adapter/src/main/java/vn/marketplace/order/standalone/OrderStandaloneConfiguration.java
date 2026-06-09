package vn.marketplace.order.standalone;

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
import vn.marketplace.order.application.order.CancelOrder;
import vn.marketplace.order.application.order.CancelOrderUc;
import vn.marketplace.order.application.order.CreatePendingOrder;
import vn.marketplace.order.application.order.CreatePendingOrderUc;
import vn.marketplace.order.application.order.GetOrder;
import vn.marketplace.order.application.order.GetOrderUc;
import vn.marketplace.order.application.order.TransitionOrder;
import vn.marketplace.order.application.order.TransitionOrderUc;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/**
 * JSON transactional-outbox wiring for the standalone profile (no Kafka) — mirrors msfw's
 * sample-service outbox configuration. Order publishes {@code OrderCompleted}/{@code OrderCancelled};
 * the {@link EventPublishingProxyCreator} drains them to {@link JsonEventStoreProcessor} (H2 outbox).
 */
@Configuration
@Import(SpringCoreConfiguration.class)
@EnableJpaRepositories(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.order.adapter.order.outbound.persistence"})
@EntityScan(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.order.adapter.order.outbound.persistence.entity"})
public class OrderStandaloneConfiguration {

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
    public CreatePendingOrder createPendingOrder(Repository<Order> orderRepository) {
        return new CreatePendingOrderUc(orderRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public TransitionOrder transitionOrder(Repository<Order> orderRepository) {
        return new TransitionOrderUc(orderRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public CancelOrder cancelOrder(Repository<Order> orderRepository) {
        return new CancelOrderUc(orderRepository);
    }

    @Bean
    public GetOrder getOrder(Repository<Order> orderRepository) {
        return new GetOrderUc(orderRepository);
    }
}
