package vn.marketplace.catalog.adapter.configuration;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.OutboxConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.springcore.configuration.SpringCoreConfiguration;
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
 * Production wiring: msfw core + Kafka-backed transactional outbox. Use-cases are declared as
 * {@code @Bean} (NOT {@code @Component}) so the msfw {@code EventPublishingProxyCreator} can wrap their
 * {@code @EventPublishHandler} methods.
 */
@Configuration
@Import({SpringCoreConfiguration.class, OutboxConfiguration.class})
@ComponentScan(basePackages = {"vn.marketplace.catalog.adapter"})
@EntityScan(basePackages = {"vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity"})
@EnableJpaRepositories(basePackages = {"vn.marketplace.catalog.adapter.product.management.outbound.persistence"})
public class AdapterConfiguration {

    @Bean
    public CreateProduct createProduct(Repository<Product> productRepository) {
        return new CreateProductUc(productRepository);
    }

    @Bean
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
    public UpdateSkuPrice updateSkuPrice(Repository<Product> productRepository) {
        return new UpdateSkuPriceUc(productRepository);
    }
}
