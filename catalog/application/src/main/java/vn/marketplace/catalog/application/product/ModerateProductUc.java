package vn.marketplace.catalog.application.product;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * Admin moderation. State-writing AND publishes {@code ProductCreated} (on first approval) → annotated
 * with {@link EventPublishHandler}; the outbox proxy drains the event to the JSON event store.
 */
public class ModerateProductUc implements ModerateProduct {

    private final Repository<Product> productRepository;

    public ModerateProductUc(Repository<Product> productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public ProductModerationView execute(ModerateProductCmd cmd) {
        Product product = productRepository.findById(new ProductId(cmd.productId()))
                .orElseThrow(() -> new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND));

        switch (cmd.action()) {
            case APPROVE -> product.approve(cmd.moderator());
            case REJECT -> product.reject(cmd.moderator(), cmd.reason());
        }

        productRepository.save(product);
        return new ProductModerationView(product.id().value(), product.status().name(),
                product.moderatedBy(), product.moderationReason(), product.moderatedAt());
    }
}
