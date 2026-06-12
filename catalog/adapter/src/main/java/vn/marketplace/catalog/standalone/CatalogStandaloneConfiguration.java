package vn.marketplace.catalog.standalone;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.StandaloneJsonOutboxConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.catalog.application.product.CreateProduct;
import vn.marketplace.catalog.application.product.CreateProductUc;
import vn.marketplace.catalog.application.product.GetPrice;
import vn.marketplace.catalog.application.product.GetPriceUc;
import vn.marketplace.catalog.application.product.GetProduct;
import vn.marketplace.catalog.application.product.GetProductUc;
import vn.marketplace.catalog.application.product.ModerateProduct;
import vn.marketplace.catalog.application.product.ModerateProductUc;
import vn.marketplace.catalog.application.product.ProductRepository;
import vn.marketplace.catalog.application.product.SearchProducts;
import vn.marketplace.catalog.application.product.SearchProductsUc;
import vn.marketplace.catalog.application.product.UpdateSkuPrice;
import vn.marketplace.catalog.application.product.UpdateSkuPriceUc;
import vn.marketplace.catalog.domain.product.management.Product;

/**
 * Standalone profile (no Kafka): msfw's {@code StandaloneJsonOutboxConfiguration} packages the
 * whole JSON outbox chain (store + property-driven routing + logging publisher); this class
 * contributes only the service's own persistence scan and use-case beans. Approving a product
 * raises {@code ProductCreated}, drained to the H2 outbox by the proxy.
 */
@Configuration
@Import(StandaloneJsonOutboxConfiguration.class)
@EnableJpaRepositories(basePackages = "vn.marketplace.catalog.adapter.product.management.outbound.persistence")
@EntityScan(basePackages = "vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity")
public class CatalogStandaloneConfiguration {

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
    public GetPrice getPrice(ProductRepository productRepository) {
        return new GetPriceUc(productRepository);
    }

    @Bean
    public GetProduct getProduct(Repository<Product> productRepository) {
        return new GetProductUc(productRepository);
    }

    @Bean
    public SearchProducts searchProducts(ProductRepository productRepository) {
        return new SearchProductsUc(productRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public UpdateSkuPrice updateSkuPrice(Repository<Product> productRepository) {
        return new UpdateSkuPriceUc(productRepository);
    }
}
