package vn.marketplace.notification.adapter.configuration;

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
import vn.marketplace.notification.adapter.delivery.inbound.messaging.NotificationEventsFacade;
import vn.marketplace.notification.adapter.delivery.inbound.messaging.PaymentReceivedData;
import tech.vsf.ptnt.springcore.configuration.SpringCoreConfiguration;
import vn.marketplace.notification.application.delivery.AcceptNotification;
import vn.marketplace.notification.application.delivery.AcceptNotificationUc;
import vn.marketplace.notification.application.delivery.DispatchNotification;
import vn.marketplace.notification.application.delivery.DispatchNotificationUc;
import vn.marketplace.notification.application.delivery.EncryptionPort;
import vn.marketplace.notification.application.delivery.GetNotification;
import vn.marketplace.notification.application.delivery.GetNotificationUc;
import vn.marketplace.notification.application.delivery.NotificationProviderPort;
import vn.marketplace.notification.application.delivery.PreferencePort;
import vn.marketplace.notification.domain.delivery.management.Notification;

/**
 * Production wiring: msfw core + transactional outbox. Use-cases are declared as {@code @Bean} (NOT
 * {@code @Component}) so the msfw {@code EventPublishingProxyCreator} can wrap their
 * {@code @EventPublishHandler} methods. The encryption / preference / provider adapters are
 * {@code @Component}s picked up by the component scan.
 */
@Configuration
@Import({SpringCoreConfiguration.class, OutboxConfiguration.class, ConsumptionConfiguration.class})
@ComponentScan(basePackages = {"vn.marketplace.notification.adapter"})
@EntityScan(basePackages = {"vn.marketplace.notification.adapter.delivery.outbound.persistence.entity"})
@EnableJpaRepositories(basePackages = {"vn.marketplace.notification.adapter.delivery.outbound.persistence"})
public class AdapterConfiguration {

    @Bean
    public AcceptNotification acceptNotification(Repository<Notification> repository, EncryptionPort encryptionPort) {
        return new AcceptNotificationUc(repository, encryptionPort);
    }

    @Bean
    public DispatchNotification dispatchNotification(Repository<Notification> repository,
                                                     PreferencePort preferencePort,
                                                     NotificationProviderPort providerPort) {
        return new DispatchNotificationUc(repository, preferencePort, providerPort);
    }

    @Bean
    public GetNotification getNotification(Repository<Notification> repository) {
        return new GetNotificationUc(repository);
    }

    /** Event subscriptions — msfw's registrar builds and registers a pipeline per entry. */
    @Bean
    public EventSubscriber notificationEventSubscriptions(NotificationEventsFacade facade) {
        return pipelines -> pipelines.subscribe("Payment", "PaymentReceived", PaymentReceivedData.class)
                .handle(facade, "onPaymentReceived");
    }
}
