package vn.marketplace.notification.application.delivery;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.notification.application.delivery.NotificationProviderPort.ProviderException;
import vn.marketplace.notification.domain.delivery.NotificationId;
import vn.marketplace.notification.domain.delivery.Priority;
import vn.marketplace.notification.domain.delivery.management.Notification;
import vn.marketplace.notification.domain.shared.NotificationDomainException;
import vn.marketplace.notification.domain.shared.NotificationErrorCode;

/**
 * Renders and attempts delivery of an accepted notification.
 *
 * <p>Opt-out (BULK only, fail-closed): if the user has opted out, the notification is moved to
 * SUPPRESSED and the provider is <strong>never called</strong>. Otherwise it is rendered and sent; a
 * provider failure records FAILED with the classified result. State-writing → {@link EventPublishHandler}.
 */
public class DispatchNotificationUc implements DispatchNotification {

    private final Repository<Notification> notificationRepository;
    private final PreferencePort preferencePort;
    private final NotificationProviderPort providerPort;

    public DispatchNotificationUc(Repository<Notification> notificationRepository,
                                  PreferencePort preferencePort,
                                  NotificationProviderPort providerPort) {
        this.notificationRepository = notificationRepository;
        this.preferencePort = preferencePort;
        this.providerPort = providerPort;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(DispatchNotificationCmd cmd) {
        Notification notification = notificationRepository.findById(new NotificationId(cmd.notificationId()))
                .orElseThrow(() -> new NotificationDomainException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        // BULK opt-out: suppress without ever calling the provider.
        if (notification.priority() == Priority.BULK && preferencePort.isOptedOut(notification.userId())) {
            notification.suppress("user opted out of BULK notifications");
            notificationRepository.save(notification);
            return;
        }

        notification.markRendered();
        try {
            String provider = providerPort.send(notification.channel(), notification.userId(),
                    notification.templateId(), notification.variables());
            notification.recordSent(provider);
        } catch (ProviderException e) {
            notification.fail(e.provider(), e.isPermanent() ? "PERMANENT_ERROR" : "TRANSIENT_ERROR");
        }
        notificationRepository.save(notification);
    }
}
