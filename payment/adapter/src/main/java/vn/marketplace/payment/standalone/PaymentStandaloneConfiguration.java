package vn.marketplace.payment.standalone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.StandaloneJsonOutboxConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
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
 * Standalone profile (no Kafka): msfw's {@code StandaloneJsonOutboxConfiguration} packages the
 * whole JSON outbox chain; this class contributes only the service's own persistence scan and
 * use-case beans. Payment publishes {@code PaymentReceived}/{@code PaymentFailed}/
 * {@code PayoutCompleted} into the H2 outbox.
 */
@Configuration
@Import(StandaloneJsonOutboxConfiguration.class)
@EnableJpaRepositories(basePackages = "vn.marketplace.payment.adapter.payment.outbound.persistence")
@EntityScan(basePackages = "vn.marketplace.payment.adapter.payment.outbound.persistence.entity")
public class PaymentStandaloneConfiguration {

    /** Commission rate in basis points — standard merchant tier 2%; tiered rates via config. */
    @Bean
    public CommissionPolicy commissionPolicy(
            @Value("${payment.commission.rate-basis-points:200}") int rateBasisPoints) {
        return new CommissionPolicy(rateBasisPoints);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public InitEscrow initEscrow(Repository<Payment> paymentRepository, PaymentGatewayPort gateway) {
        return new InitEscrowUc(paymentRepository, gateway);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public HandleWebhook handleWebhook(Repository<Payment> paymentRepository) {
        return new HandleWebhookUc(paymentRepository);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ProcessSettlement processSettlement(PaymentRepository paymentRepository,
                                               Repository<Settlement> settlementRepository,
                                               SettlementDocWriter settlementDocWriter,
                                               CommissionPolicy commissionPolicy) {
        return new ProcessSettlementUc(paymentRepository, settlementRepository, settlementDocWriter,
                commissionPolicy);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public ProcessPayout processPayout(Repository<Settlement> settlementRepository, BankPort bank) {
        return new ProcessPayoutUc(settlementRepository, bank);
    }

    @Bean
    public GetPayment getPayment(Repository<Payment> paymentRepository) {
        return new GetPaymentUc(paymentRepository);
    }
}
