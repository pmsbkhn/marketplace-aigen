package vn.marketplace.catalog.adapter.product.management.facade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.marketplace.catalog.adapter.product.management.dto.CreateProductRequest;
import vn.marketplace.catalog.adapter.product.management.dto.UpdateSkuPriceRequest;
import vn.marketplace.catalog.application.product.CreateProduct;
import vn.marketplace.catalog.application.product.GetPrice;
import vn.marketplace.catalog.application.product.GetPriceCmd;
import vn.marketplace.catalog.application.product.GetProduct;
import vn.marketplace.catalog.application.product.GetProductCmd;
import vn.marketplace.catalog.application.product.ProductIdView;
import vn.marketplace.catalog.application.product.ProductSearchResult;
import vn.marketplace.catalog.application.product.ProductView;
import vn.marketplace.catalog.application.product.SearchProducts;
import vn.marketplace.catalog.application.product.SearchProductsCmd;
import vn.marketplace.catalog.application.product.SkuPriceView;
import vn.marketplace.catalog.application.product.UpdateSkuPrice;
import vn.marketplace.catalog.application.product.UpdateSkuPriceCmd;
import vn.marketplace.catalog.domain.shared.Actor;

/** Transactional application boundary for product command/query use-cases. */
@Service
public class ProductFacade {

    private final CreateProduct createProduct;
    private final GetProduct getProduct;
    private final SearchProducts searchProducts;
    private final GetPrice getPrice;
    private final UpdateSkuPrice updateSkuPrice;

    public ProductFacade(CreateProduct createProduct,
                         GetProduct getProduct,
                         SearchProducts searchProducts,
                         GetPrice getPrice,
                         UpdateSkuPrice updateSkuPrice) {
        this.createProduct = createProduct;
        this.getProduct = getProduct;
        this.searchProducts = searchProducts;
        this.getPrice = getPrice;
        this.updateSkuPrice = updateSkuPrice;
    }

    @Transactional
    public ProductIdView create(CreateProductRequest request, Actor merchant) {
        return createProduct.execute(request.toCmd(merchant.id(), merchant.id()));
    }

    @Transactional(readOnly = true)
    public ProductView get(String productId, Actor caller) {
        return getProduct.execute(new GetProductCmd(productId, caller));
    }

    @Transactional(readOnly = true)
    public ProductSearchResult search(SearchProductsCmd cmd) {
        return searchProducts.execute(cmd);
    }

    @Transactional(readOnly = true)
    public List<SkuPriceView> prices(List<String> skuCodes) {
        return getPrice.execute(new GetPriceCmd(skuCodes));
    }

    @Transactional
    public SkuPriceView updateSkuPrice(String productId, String skuCode,
                                       UpdateSkuPriceRequest request, Actor caller) {
        return updateSkuPrice.execute(
                new UpdateSkuPriceCmd(productId, skuCode, request.priceAmount(), caller));
    }
}
