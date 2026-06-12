package vn.marketplace.inventory.standalone;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.StandaloneJsonOutboxConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.inventory.application.stock.DeductStock;
import vn.marketplace.inventory.application.stock.DeductStockUc;
import vn.marketplace.inventory.application.stock.GetStockLevel;
import vn.marketplace.inventory.application.stock.GetStockLevelUc;
import vn.marketplace.inventory.application.stock.InitSku;
import vn.marketplace.inventory.application.stock.InitSkuUc;
import vn.marketplace.inventory.application.stock.ReleaseStock;
import vn.marketplace.inventory.application.stock.ReleaseStockUc;
import vn.marketplace.inventory.application.stock.ReserveStock;
import vn.marketplace.inventory.application.stock.ReserveStockUc;
import vn.marketplace.inventory.application.stock.StockRepository;
import vn.marketplace.inventory.application.stock.UpdateMerchantStock;
import vn.marketplace.inventory.application.stock.UpdateMerchantStockUc;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Standalone profile (no Kafka): msfw's {@code StandaloneJsonOutboxConfiguration} packages the
 * whole JSON outbox chain; this class contributes only the service's own persistence scan and
 * use-case beans. Inventory publishes no domain events, but its state-writing use-cases are
 * annotated {@code @EventPublishHandler}, so the proxy + event-store beans must be present.
 */
@Configuration
@Import(StandaloneJsonOutboxConfiguration.class)
@EnableJpaRepositories(basePackages = "vn.marketplace.inventory.adapter.stock.outbound.persistence")
@EntityScan(basePackages = "vn.marketplace.inventory.adapter.stock.outbound.persistence.entity")
public class InventoryStandaloneConfiguration {

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ReserveStock reserveStock(Repository<Stock> stockRepository) {
        return new ReserveStockUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ReleaseStock releaseStock(StockRepository stockRepository) {
        return new ReleaseStockUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public DeductStock deductStock(Repository<Stock> stockRepository) {
        return new DeductStockUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public InitSku initSku(Repository<Stock> stockRepository) {
        return new InitSkuUc(stockRepository);
    }

    @Bean
    public GetStockLevel getStockLevel(Repository<Stock> stockRepository) {
        return new GetStockLevelUc(stockRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public UpdateMerchantStock updateMerchantStock(Repository<Stock> stockRepository) {
        return new UpdateMerchantStockUc(stockRepository);
    }
}
