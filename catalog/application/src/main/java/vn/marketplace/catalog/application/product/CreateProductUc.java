package vn.marketplace.catalog.application.product;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.ProductFactory;

/**
 * Creates a PENDING product. State-writing → annotated with {@link EventPublishHandler} (Event-Publish
 * fitness function). Creation itself raises no domain event ({@code ProductCreated} fires on first
 * activation), but the outbox proxy still wraps every state-changing use-case.
 */
public class CreateProductUc implements CreateProduct {

    private final Repository<Product> productRepository;
    private final ProductFactory productFactory = new ProductFactory();

    public CreateProductUc(Repository<Product> productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public ProductIdView execute(CreateProductCmd cmd) {
        Product product = productFactory.create(cmd.produceFactoryParams());
        productRepository.save(product);
        return new ProductIdView(product.id().value(), product.status().name());
    }
}
