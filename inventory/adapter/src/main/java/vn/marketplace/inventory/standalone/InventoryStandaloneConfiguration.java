package vn.marketplace.inventory.standalone;

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
import vn.marketplace.inventory.application.stock.DeductStock;
import vn.marketplace.inventory.application.stock.DeductStockUc;
import vn.marketplace.inventory.application.stock.GetStockLevel;
import vn.marketplace.inventory.application.stock.GetStockLevelUc;
import vn.marketplace.inventory.application.stock.InitSku;
import vn.marketplace.inventory.application.stock.InitSkuUc;
import vn.marketplace.inventory.application.stock.ReleaseStock;
import vn.marketplace.inventory.application.stock.ReleaseStockUc;
import vn.marketplace.inventory.application.stock.ReserveStock;
import vn.marketplace.inventory.application.stock.ReserveStockUc;
import vn.marketplace.inventory.application.stock.UpdateMerchantStock;
import vn.marketplace.inventory.application.stock.UpdateMerchantStockUc;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * JSON-only transactional-outbox wiring for the standalone profile (no Kafka) — mirrors msfw's
 * sample-service outbox configuration (storage side only). Inventory publishes no domain events, but
 * its state-writing use-cases are annotated {@code @EventPublishHandler}, so the
 * {@link EventPublishingProxyCreator} + JSON event-store beans must be present for the proxy to run.
 */
@Configuration
@Import(SpringCoreConfiguration.class)
@EnableJpaRepositories(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.inventory.adapter.stock.outbound.persistence"})
@EntityScan(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.inventory.adapter.stock.outbound.persistence.entity"})
public class InventoryStandaloneConfiguration {

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
    public ReserveStock reserveStock(Repository<Stock> stockRepository) {
        return new ReserveStockUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ReleaseStock releaseStock(Repository<Stock> stockRepository) {
        return new ReleaseStockUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public DeductStock deductStock(Repository<Stock> stockRepository) {
        return new DeductStockUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public InitSku initSku(Repository<Stock> stockRepository) {
        return new InitSkuUc(stockRepository);
    }

    @Bean
    public GetStockLevel getStockLevel(Repository<Stock> stockRepository) {
        return new GetStockLevelUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public UpdateMerchantStock updateMerchantStock(Repository<Stock> stockRepository) {
        return new UpdateMerchantStockUc(stockRepository);
    }
}
