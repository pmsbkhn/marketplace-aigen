package vn.marketplace.catalog.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.catalog.adapter.configuration.AdapterConfiguration;

/**
 * In-cluster entry point — boots Catalog against the real PostgreSQL in the {@code infra} namespace,
 * with NO Spring Cloud Config server (config from {@code application-k8s.properties} + env/ConfigMap).
 *
 * <p>Reuses {@link CatalogStandaloneConfiguration} for the JSON transactional-outbox wiring (events
 * persisted to Postgres). Kafka-backed publishing is wired in Phase C. Run with
 * {@code SPRING_PROFILES_ACTIVE=k8s} (the image sets that + the bootstrap name below).
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.catalog.k8s")
@Import(AdapterConfiguration.class)
public class CatalogK8sApplication {
    public static void main(String[] args) {
        // Legacy bootstrap (forced by spring-cloud-starter-bootstrap) ignores the active profile;
        // point it at bootstrap-k8s.yml: optional configserver import + config client disabled.
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(CatalogK8sApplication.class, args);
    }
}
