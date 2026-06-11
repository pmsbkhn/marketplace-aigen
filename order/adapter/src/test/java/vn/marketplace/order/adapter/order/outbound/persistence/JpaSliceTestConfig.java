package vn.marketplace.order.adapter.order.outbound.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal {@code @SpringBootConfiguration} for the {@code @DataJpaTest} slice tests in this package
 * — JPA + H2 only, none of the msfw/Kafka wiring of the real applications.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "vn.marketplace.order.adapter.order.outbound.persistence")
@EntityScan(basePackages = "vn.marketplace.order.adapter.order.outbound.persistence.entity")
class JpaSliceTestConfig {
}
