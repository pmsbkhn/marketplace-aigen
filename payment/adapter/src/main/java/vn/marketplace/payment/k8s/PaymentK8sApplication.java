package vn.marketplace.payment.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.payment.adapter.configuration.AdapterConfiguration;

/**
 * In-cluster entry point — boots Payment against the real PostgreSQL in the {@code infra} namespace,
 * no config server. Reuses {@link PaymentStandaloneConfiguration} (JSON outbox; the WORM settlement
 * store and gateway/bank clients remain local stand-ins). Run with {@code SPRING_PROFILES_ACTIVE=k8s}.
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.payment.k8s")
@Import(AdapterConfiguration.class)
public class PaymentK8sApplication {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(PaymentK8sApplication.class, args);
    }
}
