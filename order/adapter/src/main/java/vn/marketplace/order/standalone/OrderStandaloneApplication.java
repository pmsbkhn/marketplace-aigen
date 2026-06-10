package vn.marketplace.order.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Standalone entry point — boots Order with ZERO external infrastructure (H2 + JSON
 * transactional-outbox, no config server, no Kafka). Wires the JSON outbox via
 * {@link OrderStandaloneConfiguration} (NOT the production AdapterConfiguration which imports the
 * Kafka-backed OutboxConfiguration).
 *
 * <p>Run: {@code mvn -pl adapter spring-boot:run
 *   -Dspring-boot.run.main-class=vn.marketplace.order.standalone.OrderStandaloneApplication
 *   -Dspring-boot.run.profiles=standalone}
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.order.adapter.order")
@Import(OrderStandaloneConfiguration.class)
public class OrderStandaloneApplication {
    public static void main(String[] args) {
        // Legacy bootstrap (forced by spring-cloud-starter-bootstrap) ignores the active profile;
        // point it at bootstrap-standalone.yml: optional configserver import + config client disabled.
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-standalone");
        SpringApplication.run(OrderStandaloneApplication.class, args);
    }
}
