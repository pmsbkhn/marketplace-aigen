package vn.marketplace.checkout.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.checkout.adapter.configuration.AdapterConfiguration;

/**
 * Production entry point — loads config from Spring Cloud Config. Requires Redis + the config
 * server (no database, no Kafka). For a no-infra run use
 * {@code vn.marketplace.checkout.standalone.CheckoutStandaloneApplication}.
 */
@SpringBootApplication(excludeName = {
        // spring-adapter-core drags JPA onto the classpath; Checkout has NO database (stateless saga)
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"})
@Import(AdapterConfiguration.class)
public class CheckoutServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }
}
