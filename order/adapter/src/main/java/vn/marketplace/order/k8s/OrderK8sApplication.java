package vn.marketplace.order.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.order.standalone.OrderStandaloneConfiguration;

/**
 * In-cluster entry point — boots Order against the real PostgreSQL in the {@code infra} namespace,
 * no config server. Reuses {@link OrderStandaloneConfiguration} (JSON outbox). Run with
 * {@code SPRING_PROFILES_ACTIVE=k8s}.
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.order.adapter.order")
@Import(OrderStandaloneConfiguration.class)
public class OrderK8sApplication {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(OrderK8sApplication.class, args);
    }
}
