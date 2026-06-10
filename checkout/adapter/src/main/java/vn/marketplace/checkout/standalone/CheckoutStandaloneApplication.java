package vn.marketplace.checkout.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.checkout.adapter.configuration.AdapterConfiguration;

/**
 * Standalone entry point — boots Checkout with ZERO external infrastructure (in-memory session
 * store, no config server, no Redis). The wiring is the same {@link AdapterConfiguration} as
 * production — Checkout has no outbox/Kafka to swap; only the config-server bootstrap differs.
 *
 * <p>Run: {@code mvn -pl adapter spring-boot:run
 *   -Dspring-boot.run.main-class=vn.marketplace.checkout.standalone.CheckoutStandaloneApplication
 *   -Dspring-boot.run.profiles=standalone}
 */
@SpringBootApplication(excludeName = {
        // spring-adapter-core drags JPA onto the classpath; Checkout has NO database (stateless saga)
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"})
@Import(AdapterConfiguration.class)
public class CheckoutStandaloneApplication {
    public static void main(String[] args) {
        // spring-cloud-starter-bootstrap's marker forces the legacy bootstrap phase, whose context
        // ignores the active profile and reads bootstrap.yml (non-optional configserver import).
        // Point it at bootstrap-standalone.yml instead: optional import + config client disabled.
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-standalone");
        SpringApplication.run(CheckoutStandaloneApplication.class, args);
    }
}
