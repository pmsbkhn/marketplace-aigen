package vn.marketplace.notification.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.notification.adapter.configuration.AdapterConfiguration;

/**
 * Production entry point — loads config from Spring Cloud Config and uses the transactional outbox.
 * Requires PostgreSQL + Kafka + config server. For a no-infra run use
 * {@code vn.marketplace.notification.standalone.NotificationStandaloneApplication}.
 */
@SpringBootApplication
@Import(AdapterConfiguration.class)
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
