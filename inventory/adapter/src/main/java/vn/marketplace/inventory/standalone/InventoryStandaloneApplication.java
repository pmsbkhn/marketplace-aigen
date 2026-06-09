package vn.marketplace.inventory.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Standalone entry point — boots Inventory with ZERO external infrastructure (H2 + JSON
 * transactional-outbox, no config server, no Kafka).
 *
 * <p>Scans only the stock web/facade/persistence beans and wires the JSON outbox via
 * {@link InventoryStandaloneConfiguration} (deliberately NOT the production AdapterConfiguration, which
 * imports the Kafka-backed OutboxConfiguration).
 *
 * <p>Run: {@code mvn -pl adapter spring-boot:run
 *   -Dspring-boot.run.main-class=vn.marketplace.inventory.standalone.InventoryStandaloneApplication
 *   -Dspring-boot.run.profiles=standalone}
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.inventory.adapter.stock")
@Import(InventoryStandaloneConfiguration.class)
public class InventoryStandaloneApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryStandaloneApplication.class, args);
    }
}
