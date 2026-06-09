package vn.marketplace.catalog.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Standalone entry point — boots Catalog with ZERO external infrastructure (H2 + JSON
 * transactional-outbox, no config server, no Kafka).
 *
 * <p>Scans only the product web/facade/persistence beans and wires the JSON outbox via
 * {@link CatalogStandaloneConfiguration} (deliberately NOT the production AdapterConfiguration, which
 * imports the Kafka-backed OutboxConfiguration).
 *
 * <p>Run: {@code mvn -pl adapter spring-boot:run
 *   -Dspring-boot.run.main-class=vn.marketplace.catalog.standalone.CatalogStandaloneApplication
 *   -Dspring-boot.run.profiles=standalone}
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.catalog.adapter.product")
@Import(CatalogStandaloneConfiguration.class)
public class CatalogStandaloneApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogStandaloneApplication.class, args);
    }
}
