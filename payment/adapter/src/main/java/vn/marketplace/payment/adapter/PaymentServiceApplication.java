package vn.marketplace.payment.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.payment.adapter.configuration.AdapterConfiguration;

/**
 * Production entry point — loads config from Spring Cloud Config and publishes via the Kafka-backed
 * outbox. Requires PostgreSQL + Kafka + config server. For a no-infra run use
 * {@code vn.marketplace.payment.standalone.PaymentStandaloneApplication}.
 */
@SpringBootApplication
@Import(AdapterConfiguration.class)
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
