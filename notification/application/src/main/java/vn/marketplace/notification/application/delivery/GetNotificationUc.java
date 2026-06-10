package vn.marketplace.notification.application.delivery;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.notification.domain.delivery.NotificationId;
import vn.marketplace.notification.domain.delivery.management.Notification;
import vn.marketplace.notification.domain.shared.NotificationDomainException;
import vn.marketplace.notification.domain.shared.NotificationErrorCode;

/** Reads a notification's status. Read-only — no event handler. */
public class GetNotificationUc implements GetNotification {

    private final Repository<Notification> notificationRepository;

    public GetNotificationUc(Repository<Notification> notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationStatusView execute(GetNotificationCmd cmd) {
        Notification notification = notificationRepository.findById(new NotificationId(cmd.notificationId()))
                .orElseThrow(() -> new NotificationDomainException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        return new NotificationStatusView(notification.id().value(), notification.status().name(),
                notification.attempts().size());
    }
}
