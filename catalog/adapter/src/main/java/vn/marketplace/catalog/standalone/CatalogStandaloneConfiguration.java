package vn.marketplace.catalog.standalone;

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
import vn.marketplace.catalog.application.product.CreateProduct;
import vn.marketplace.catalog.application.product.CreateProductUc;
import vn.marketplace.catalog.application.product.GetPrice;
import vn.marketplace.catalog.application.product.GetPriceUc;
import vn.marketplace.catalog.application.product.GetProduct;
import vn.marketplace.catalog.application.product.GetProductUc;
import vn.marketplace.catalog.application.product.ModerateProduct;
import vn.marketplace.catalog.application.product.ModerateProductUc;
import vn.marketplace.catalog.application.product.SearchProducts;
import vn.marketplace.catalog.application.product.SearchProductsUc;
import vn.marketplace.catalog.application.product.UpdateSkuPrice;
import vn.marketplace.catalog.application.product.UpdateSkuPriceUc;
import vn.marketplace.catalog.domain.product.management.Product;

/**
 * JSON-only transactional-outbox wiring for the standalone profile (no Kafka) — mirrors msfw's
 * sample-service outbox configuration (storage side only). Approving a product raises
 * {@code ProductCreated}; the {@link EventPublishingProxyCreator} drains it to
 * {@link JsonEventStoreProcessor}, which persists an outbox row to H2.
 */
@Configuration
@Import(SpringCoreConfiguration.class)
@EnableJpaRepositories(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.catalog.adapter.product.management.outbound.persistence"})
@EntityScan(basePackages = {
        "tech.vsf.ptnt.msfw.infrastructure.persistence",
        "vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity"})
public class CatalogStandaloneConfiguration {

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
    public CreateProduct createProduct(Repository<Product> productRepository) {
        return new CreateProductUc(productRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ModerateProduct moderateProduct(Repository<Product> productRepository) {
        return new ModerateProductUc(productRepository);
    }

    @Bean
    public GetPrice getPrice(Repository<Product> productRepository) {
        return new GetPriceUc(productRepository);
    }

    @Bean
    public GetProduct getProduct(Repository<Product> productRepository) {
        return new GetProductUc(productRepository);
    }

    @Bean
    public SearchProducts searchProducts(Repository<Product> productRepository) {
        return new SearchProductsUc(productRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public UpdateSkuPrice updateSkuPrice(Repository<Product> productRepository) {
        return new UpdateSkuPriceUc(productRepository);
    }
}
