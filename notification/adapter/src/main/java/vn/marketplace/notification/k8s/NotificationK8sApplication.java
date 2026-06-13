package vn.marketplace.notification.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.notification.adapter.configuration.AdapterConfiguration;

/**
 * In-cluster entry point — boots Notification against the real PostgreSQL in the {@code infra}
 * namespace, no config server. Reuses {@link NotificationStandaloneConfiguration} (JSON outbox). Run
 * with {@code SPRING_PROFILES_ACTIVE=k8s}.
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.notification.k8s")
@Import(AdapterConfiguration.class)
public class NotificationK8sApplication {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(NotificationK8sApplication.class, args);
    }
}
