package vn.marketplace.payment.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Standalone entry point — boots Payment with ZERO external infrastructure (H2 + JSON
 * transactional-outbox, no config server, no Kafka, no real gateway/bank/S3). Wires the JSON outbox
 * via {@link PaymentStandaloneConfiguration} (NOT the production AdapterConfiguration which imports
 * the Kafka-backed OutboxConfiguration).
 *
 * <p>Run: {@code mvn -pl adapter spring-boot:run
 *   -Dspring-boot.run.main-class=vn.marketplace.payment.standalone.PaymentStandaloneApplication
 *   -Dspring-boot.run.profiles=standalone}
 * (bootstrap-standalone.yml makes the config-server import optional — no infra needed)
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.payment.adapter.payment")
@Import(PaymentStandaloneConfiguration.class)
public class PaymentStandaloneApplication {
    public static void main(String[] args) {
        // spring-cloud-starter-bootstrap's marker forces the legacy bootstrap phase, whose context
        // ignores the active profile and reads bootstrap.yml (non-optional configserver import).
        // Point it at bootstrap-standalone.yml instead: optional import + config client disabled.
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-standalone");
        SpringApplication.run(PaymentStandaloneApplication.class, args);
    }
}
