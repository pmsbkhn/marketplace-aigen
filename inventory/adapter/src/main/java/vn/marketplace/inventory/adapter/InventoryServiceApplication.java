package vn.marketplace.inventory.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.inventory.adapter.configuration.AdapterConfiguration;

/**
 * Production entry point — loads config from Spring Cloud Config and publishes via the Kafka-backed
 * outbox. Requires PostgreSQL + Kafka + config server. For a no-infra run use
 * {@code vn.marketplace.inventory.standalone.InventoryStandaloneApplication}.
 */
@SpringBootApplication
@Import(AdapterConfiguration.class)
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
