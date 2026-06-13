package vn.marketplace.order.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.order.adapter.configuration.AdapterConfiguration;

/**
 * In-cluster entry point — the production wiring ({@link AdapterConfiguration}: Kafka-backed
 * outbox + consumption) but with {@code bootstrap-k8s} so no Spring Cloud Config server is
 * required (config comes from env/ConfigMap). Run with {@code SPRING_PROFILES_ACTIVE=k8s} and
 * {@code SPRING_KAFKA_BOOTSTRAP_SERVERS} pointing at the in-cluster broker.
 */
@SpringBootApplication(scanBasePackages = "vn.marketplace.order.k8s")
@Import(AdapterConfiguration.class)
public class OrderK8sApplication {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(OrderK8sApplication.class, args);
    }
}
