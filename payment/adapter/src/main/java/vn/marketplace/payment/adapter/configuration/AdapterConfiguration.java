package vn.marketplace.payment.adapter.configuration;

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
import vn.marketplace.payment.adapter.payment.dto.OrderCompletedEvent;
import vn.marketplace.payment.adapter.payment.inbound.messaging.OrderEventsFacade;
import tech.vsf.ptnt.springcore.configuration.SpringCoreConfiguration;
import vn.marketplace.payment.application.payment.BankPort;
import vn.marketplace.payment.application.payment.GetPayment;
import vn.marketplace.payment.application.payment.GetPaymentUc;
import vn.marketplace.payment.application.payment.HandleWebhook;
import vn.marketplace.payment.application.payment.HandleWebhookUc;
import vn.marketplace.payment.application.payment.InitEscrow;
import vn.marketplace.payment.application.payment.InitEscrowUc;
import vn.marketplace.payment.application.payment.PaymentGatewayPort;
import vn.marketplace.payment.application.payment.PaymentRepository;
import vn.marketplace.payment.application.payment.ProcessPayout;
import vn.marketplace.payment.application.payment.ProcessPayoutUc;
import vn.marketplace.payment.application.payment.ProcessSettlement;
import vn.marketplace.payment.application.payment.ProcessSettlementUc;
import vn.marketplace.payment.application.payment.SettlementDocWriter;
import vn.marketplace.payment.domain.payment.management.CommissionPolicy;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.payment.management.Settlement;

/**
 * Production wiring: msfw core + Kafka-backed transactional outbox (Payment publishes
 * PaymentReceived / PaymentFailed / PayoutCompleted). Use-cases are declared as {@code @Bean}
 * (NOT {@code @Component}) so the msfw {@code EventPublishingProxyCreator} can wrap their
 * {@code @EventPublishHandler} methods.
 */
@Configuration
@Import({SpringCoreConfiguration.class, OutboxConfiguration.class, ConsumptionConfiguration.class})
@ComponentScan(basePackages = {"vn.marketplace.payment.adapter"})
@EntityScan(basePackages = {"vn.marketplace.payment.adapter.payment.outbound.persistence.entity"})
@EnableJpaRepositories(basePackages = {"vn.marketplace.payment.adapter.payment.outbound.persistence"})
public class AdapterConfiguration {

    /** Commission rate in basis points — standard merchant tier 2%; tiered rates via config. */
    @Bean
    public CommissionPolicy commissionPolicy(
            @Value("${payment.commission.rate-basis-points:200}") int rateBasisPoints) {
        return new CommissionPolicy(rateBasisPoints);
    }

    @Bean
    public InitEscrow initEscrow(Repository<Payment> paymentRepository, PaymentGatewayPort gateway) {
        return new InitEscrowUc(paymentRepository, gateway);
    }

    @Bean
    public HandleWebhook handleWebhook(Repository<Payment> paymentRepository) {
        return new HandleWebhookUc(paymentRepository);
    }

    @Bean
    public ProcessSettlement processSettlement(PaymentRepository paymentRepository,
                                               Repository<Settlement> settlementRepository,
                                               SettlementDocWriter settlementDocWriter,
                                               CommissionPolicy commissionPolicy) {
        return new ProcessSettlementUc(paymentRepository, settlementRepository, settlementDocWriter,
                commissionPolicy);
    }

    @Bean
    public ProcessPayout processPayout(Repository<Settlement> settlementRepository, BankPort bank) {
        return new ProcessPayoutUc(settlementRepository, bank);
    }

    @Bean
    public GetPayment getPayment(Repository<Payment> paymentRepository) {
        return new GetPaymentUc(paymentRepository);
    }

    /** Event subscriptions — msfw's registrar builds and registers a pipeline per entry. */
    @Bean
    public EventSubscriber orderEventSubscriptions(OrderEventsFacade facade) {
        return pipelines -> pipelines.subscribe("Order", "OrderCompleted", OrderCompletedEvent.class)
                .handle(facade, "onOrderCompleted");
    }
}
