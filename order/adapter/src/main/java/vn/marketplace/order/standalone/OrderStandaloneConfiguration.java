package vn.marketplace.order.standalone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.StandaloneJsonOutboxConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.order.application.order.CancelOrder;
import vn.marketplace.order.application.order.CancelOrderUc;
import vn.marketplace.order.application.order.CreatePendingOrder;
import vn.marketplace.order.application.order.CreatePendingOrderUc;
import vn.marketplace.order.application.order.ExpirePendingOrder;
import vn.marketplace.order.application.order.ExpirePendingOrderUc;
import vn.marketplace.order.application.order.GetOrder;
import vn.marketplace.order.application.order.GetOrderUc;
import vn.marketplace.order.application.order.TransitionOrder;
import vn.marketplace.order.application.order.TransitionOrderUc;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/**
 * Standalone profile (no Kafka): msfw's {@code StandaloneJsonOutboxConfiguration} packages the
 * whole JSON outbox chain; this class contributes only the service's own persistence scan and
 * use-case beans. Order publishes {@code OrderCompleted}/{@code OrderCancelled} into the H2 outbox.
 */
@Configuration
@Import(StandaloneJsonOutboxConfiguration.class)
@EnableJpaRepositories(basePackages = "vn.marketplace.order.adapter.order.outbound.persistence")
@EntityScan(basePackages = "vn.marketplace.order.adapter.order.outbound.persistence.entity")
public class OrderStandaloneConfiguration {

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public CreatePendingOrder createPendingOrder(Repository<Order> orderRepository,
                                                 @Value("${order.pending-expiry-min:30}") int pendingExpiryMinutes) {
        return new CreatePendingOrderUc(orderRepository, pendingExpiryMinutes);
    }

    /** FR13 auto-cancel: handler of the {@code OrderPendingTimedOut} delayed timer. */
    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ExpirePendingOrder expirePendingOrder(Repository<Order> orderRepository) {
        return new ExpirePendingOrderUc(orderRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public TransitionOrder transitionOrder(Repository<Order> orderRepository) {
        return new TransitionOrderUc(orderRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public CancelOrder cancelOrder(Repository<Order> orderRepository) {
        return new CancelOrderUc(orderRepository);
    }

    @Bean
    public GetOrder getOrder(Repository<Order> orderRepository) {
        return new GetOrderUc(orderRepository);
    }
}
