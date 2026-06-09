package vn.marketplace.catalog.application.product;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * Reads a product, enforcing visibility:
 * <ul>
 *   <li>Admin: any product.</li>
 *   <li>Merchant: only their own products (any status) — else 404 (IDOR-safe).</li>
 *   <li>Buyer/public: only ACTIVE products — else 404.</li>
 * </ul>
 * Read-only — no event handler.
 */
public class GetProductUc implements GetProduct {

    private final Repository<Product> productRepository;

    public GetProductUc(Repository<Product> productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductView execute(GetProductCmd cmd) {
        Product product = productRepository.findById(new ProductId(cmd.productId()))
                .orElseThrow(() -> new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND));

        if (!isVisibleTo(product, cmd.caller())) {
            // 404 (not 403) to avoid leaking existence across tenants / moderation state.
            throw new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND);
        }
        return ProductViewMapper.toDetail(product);
    }

    private boolean isVisibleTo(Product product, Actor caller) {
        if (caller == null) {
            return product.isActive();
        }
        return switch (caller.role()) {
            case ADMIN -> true;
            case MERCHANT -> product.merchantId().value().equals(caller.id());
            case BUYER -> product.isActive();
        };
    }
}
