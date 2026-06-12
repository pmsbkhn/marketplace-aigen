package vn.marketplace.order.adapter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.OutboxConfiguration;
import tech.vsf.ptnt.msfw.consumption.spring.ConsumptionConfiguration;
import tech.vsf.ptnt.msfw.consumption.spring.EventSubscriber;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.order.adapter.order.inbound.messaging.OrderPendingTimedOutData;
import vn.marketplace.order.adapter.order.inbound.messaging.OrderTimeoutsFacade;
import vn.marketplace.order.adapter.order.inbound.messaging.PaymentEventsFacade;
import vn.marketplace.order.adapter.order.inbound.messaging.PaymentFailedData;
import vn.marketplace.order.adapter.order.inbound.messaging.PaymentReceivedData;
import tech.vsf.ptnt.springcore.configuration.SpringCoreConfiguration;
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
 * Production wiring: msfw core + Kafka-backed transactional outbox (Order publishes OrderCompleted /
 * OrderCancelled) + {@code ConsumptionConfiguration} (Order Worker: consumes Payment's
 * PaymentReceived / PaymentFailed). Use-cases are declared as {@code @Bean} (NOT {@code @Component})
 * so the msfw {@code EventPublishingProxyCreator} can wrap their {@code @EventPublishHandler} methods.
 */
@Configuration
@Import({SpringCoreConfiguration.class, OutboxConfiguration.class, ConsumptionConfiguration.class})
@ComponentScan(basePackages = {"vn.marketplace.order.adapter"})
@EntityScan(basePackages = {"vn.marketplace.order.adapter.order.outbound.persistence.entity"})
@EnableJpaRepositories(basePackages = {"vn.marketplace.order.adapter.order.outbound.persistence"})
public class AdapterConfiguration {

    @Bean
    public CreatePendingOrder createPendingOrder(Repository<Order> orderRepository,
                                                 @Value("${order.pending-expiry-min:30}") int pendingExpiryMinutes) {
        return new CreatePendingOrderUc(orderRepository, pendingExpiryMinutes);
    }

    /** FR13 auto-cancel: handler of the {@code OrderPendingTimedOut} delayed timer. */
    @Bean
    public ExpirePendingOrder expirePendingOrder(Repository<Order> orderRepository) {
        return new ExpirePendingOrderUc(orderRepository);
    }

    @Bean
    public TransitionOrder transitionOrder(Repository<Order> orderRepository) {
        return new TransitionOrderUc(orderRepository);
    }

    @Bean
    public CancelOrder cancelOrder(Repository<Order> orderRepository) {
        return new CancelOrderUc(orderRepository);
    }

    @Bean
    public GetOrder getOrder(Repository<Order> orderRepository) {
        return new GetOrderUc(orderRepository);
    }

    /** Event subscriptions — msfw's registrar builds and registers a pipeline per entry. */
    @Bean
    public EventSubscriber paymentEventSubscriptions(PaymentEventsFacade facade) {
        return pipelines -> {
            pipelines.subscribe("Payment", "PaymentReceived", PaymentReceivedData.class)
                    .handle(facade, "onPaymentReceived");
            pipelines.subscribe("Payment", "PaymentFailed", PaymentFailedData.class)
                    .handle(facade, "onPaymentFailed");
        };
    }

    /**
     * Our own delayed timers, coming back at their deliver-after instant. Routing for
     * {@code Order/OrderPendingTimedOut} must be {@code direction=both} (this service publishes
     * AND consumes its own timer topic).
     */
    @Bean
    public EventSubscriber orderTimeoutSubscriptions(OrderTimeoutsFacade facade) {
        return pipelines -> pipelines.subscribe("Order", "OrderPendingTimedOut", OrderPendingTimedOutData.class)
                .handle(facade, "onPendingTimedOut");
    }
}
