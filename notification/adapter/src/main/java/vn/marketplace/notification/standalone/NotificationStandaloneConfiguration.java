package vn.marketplace.notification.standalone;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.configuration.StandaloneJsonOutboxConfiguration;
import tech.vsf.ptnt.msfw.domain.core.Repository;
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
 * Standalone profile (no Kafka): msfw's {@code StandaloneJsonOutboxConfiguration} packages the
 * whole JSON outbox chain; this class contributes only the service's own persistence scan and
 * use-case beans. Notification publishes no domain events, but its state-writing use-cases are
 * annotated {@code @EventPublishHandler}, so the proxy + event-store beans must be present.
 */
@Configuration
@Import(StandaloneJsonOutboxConfiguration.class)
@EnableJpaRepositories(basePackages = "vn.marketplace.notification.adapter.delivery.outbound.persistence")
@EntityScan(basePackages = "vn.marketplace.notification.adapter.delivery.outbound.persistence.entity")
public class NotificationStandaloneConfiguration {

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public AcceptNotification acceptNotification(Repository<Notification> repository, EncryptionPort encryptionPort) {
        return new AcceptNotificationUc(repository, encryptionPort);
    }

    @Bean
    @DependsOn({"eventProcessorManager", "jsonEventStoreProcessor"})
    public DispatchNotification dispatchNotification(Repository<Notification> repository,
                                                     PreferencePort preferencePort,
                                                     NotificationProviderPort providerPort) {
        return new DispatchNotificationUc(repository, preferencePort, providerPort);
    }

    @Bean
    public GetNotification getNotification(Repository<Notification> repository) {
        return new GetNotificationUc(repository);
    }
}
