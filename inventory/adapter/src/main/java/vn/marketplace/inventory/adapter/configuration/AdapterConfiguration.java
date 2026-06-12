package vn.marketplace.inventory.adapter.configuration;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.OutboxConfiguration;
import tech.vsf.ptnt.msfw.consumption.spring.ConsumptionConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.springcore.configuration.SpringCoreConfiguration;
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
 * Production wiring: msfw core + Kafka-backed transactional outbox (producer side) +
 * {@code ConsumptionConfiguration} (consumer side: @KafkaListener over routed topics, default
 * JSON/Avro CloudEvent deserializers, inbox idempotency, retry/DLQ). Use-cases are declared as
 * {@code @Bean} (NOT {@code @Component}) so the msfw {@code EventPublishingProxyCreator} can wrap their
 * {@code @EventPublishHandler} methods (the outbox proxy path the Event-Publish fitness function requires).
 */
@Configuration
@Import({SpringCoreConfiguration.class, OutboxConfiguration.class, ConsumptionConfiguration.class})
@ComponentScan(basePackages = {"vn.marketplace.inventory.adapter"})
@EntityScan(basePackages = {"vn.marketplace.inventory.adapter.stock.outbound.persistence.entity"})
@EnableJpaRepositories(basePackages = {"vn.marketplace.inventory.adapter.stock.outbound.persistence"})
public class AdapterConfiguration {

    @Bean
    public ReserveStock reserveStock(Repository<Stock> stockRepository) {
        return new ReserveStockUc(stockRepository);
    }

    @Bean
    public ReleaseStock releaseStock(StockRepository stockRepository) {
        return new ReleaseStockUc(stockRepository);
    }

    @Bean
    public DeductStock deductStock(Repository<Stock> stockRepository) {
        return new DeductStockUc(stockRepository);
    }

    @Bean
    public InitSku initSku(Repository<Stock> stockRepository) {
        return new InitSkuUc(stockRepository);
    }

    @Bean
    public GetStockLevel getStockLevel(Repository<Stock> stockRepository) {
        return new GetStockLevelUc(stockRepository);
    }

    @Bean
    public UpdateMerchantStock updateMerchantStock(Repository<Stock> stockRepository) {
        return new UpdateMerchantStockUc(stockRepository);
    }
}
