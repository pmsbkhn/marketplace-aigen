package vn.marketplace.catalog.domain.product.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.AbstractDomainEvent;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.domain.type.DTime;

/**
 * Published exactly once, when a product first becomes ACTIVE. Inventory consumes it to initialise
 * each SKU at quantity 0 (see SAD: {@code ProductCreated → Inventory.InitSku}).
 */
public class ProductCreated extends AbstractDomainEvent {

    private final String productId;
    private final String merchantId;
    private final List<String> skuCodes;

    public ProductCreated(Product product, DTime occurredAt) {
        super(DomainEventType.of("Catalog", "ProductCreated"), occurredAt);
        this.productId = product.id().value();
        this.merchantId = product.merchantId().value();
        this.skuCodes = product.variants().stream()
                .flatMap(v -> v.skus().stream())
                .map(s -> s.code().value())
                .toList();
        this.subject = this.productId;
    }

    public String productId() {
        return productId;
    }

    public String merchantId() {
        return merchantId;
    }

    public List<String> skuCodes() {
        return skuCodes;
    }
}
