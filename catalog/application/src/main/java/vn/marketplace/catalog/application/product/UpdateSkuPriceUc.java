package vn.marketplace.catalog.application.product;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.Sku;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * Merchant changes a SKU's price (flow 5.4). State-writing → {@link EventPublishHandler} (outbox path,
 * enforced by the Event-Publish fitness function). Tenant scope: only the owning Merchant may change
 * the price — a cross-tenant caller gets {@code TENANT_MISMATCH}. Price &gt; 0 is validated in the domain.
 *
 * <p>The SKU price is the source of truth for Checkout's GetPrice; an ACTIVE product would be re-indexed
 * into the read model (ES) downstream — modelled here as the persisted state change + outbox event.
 */
public class UpdateSkuPriceUc implements UpdateSkuPrice {

    private final Repository<Product> productRepository;

    public UpdateSkuPriceUc(Repository<Product> productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public SkuPriceView execute(UpdateSkuPriceCmd cmd) {
        Product product = productRepository.findById(new ProductId(cmd.productId()))
                .orElseThrow(() -> new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND));

        Actor caller = cmd.caller();
        if (caller == null || !product.merchantId().value().equals(caller.id())) {
            throw new CatalogDomainException(CatalogErrorCode.TENANT_MISMATCH);
        }

        SkuCode code = new SkuCode(cmd.skuCode());
        product.changeSkuPrice(code, cmd.newAmount());
        productRepository.save(product);

        Sku sku = product.findSku(code).orElseThrow(
                () -> new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND, "SKU not found: " + cmd.skuCode()));
        return new SkuPriceView(sku.code().value(), sku.price().amount(), sku.price().currency().name(),
                product.merchantId().value(), product.isActive());
    }
}
