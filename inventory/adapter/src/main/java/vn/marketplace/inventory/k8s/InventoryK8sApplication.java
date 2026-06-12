package vn.marketplace.inventory.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.inventory.standalone.InventoryStandaloneConfiguration;

/**
 * In-cluster entry point — boots Inventory against the real PostgreSQL in the {@code infra} namespace,
 * no config server. Reuses {@link InventoryStandaloneConfiguration} (JSON outbox). Run with
 * {@code SPRING_PROFILES_ACTIVE=k8s}.
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.inventory.adapter.stock")
@Import(InventoryStandaloneConfiguration.class)
public class InventoryK8sApplication {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(InventoryK8sApplication.class, args);
    }
}
