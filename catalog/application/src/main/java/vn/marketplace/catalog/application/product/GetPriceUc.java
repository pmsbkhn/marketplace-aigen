package vn.marketplace.catalog.application.product;

import java.util.ArrayList;
import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.Sku;

/**
 * Reads price from the DB source of truth (never ES) for checkout consistency. Read-only — no
 * {@link tech.vsf.ptnt.msfw.event.handling.EventPublishHandler}.
 */
public class GetPriceUc implements GetPrice {

    private final Repository<Product> productRepository;

    public GetPriceUc(Repository<Product> productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<SkuPriceView> execute(GetPriceCmd cmd) {
        List<Product> products = productRepository.findBy(ProductCriteria.bySkuCodes(cmd.skuCodes()));
        List<SkuPriceView> result = new ArrayList<>();

        for (String code : cmd.skuCodes()) {
            result.add(resolve(code, products));
        }
        return result;
    }

    private SkuPriceView resolve(String code, List<Product> products) {
        SkuCode skuCode;
        try {
            skuCode = new SkuCode(code);
        } catch (IllegalArgumentException e) {
            return missing(code);
        }
        for (Product product : products) {
            var sku = product.findSku(skuCode);
            if (sku.isPresent()) {
                Sku s = sku.get();
                return new SkuPriceView(code, s.price().amount(), s.price().currency().name(),
                        product.merchantId().value(), product.isActive());
            }
        }
        return missing(code);
    }

    private SkuPriceView missing(String code) {
        return new SkuPriceView(code, 0L, "VND", null, false);
    }
}
