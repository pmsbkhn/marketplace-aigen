package vn.marketplace.notification.application.delivery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.notification.domain.delivery.Channel;
import vn.marketplace.notification.domain.delivery.NotificationId;
import vn.marketplace.notification.domain.delivery.Priority;
import vn.marketplace.notification.domain.delivery.management.Notification;

/**
 * Durably accepts a notification (status ACCEPTED). Sensitive template variables are encrypted via the
 * {@link EncryptionPort} BEFORE the aggregate is built, so the payload reaches the repository as
 * ciphertext (L4 invariant). Idempotent by {@code idempotencyKey}. State-writing →
 * {@link EventPublishHandler}.
 */
public class AcceptNotificationUc implements AcceptNotification {

    private final Repository<Notification> notificationRepository;
    private final EncryptionPort encryptionPort;

    public AcceptNotificationUc(Repository<Notification> notificationRepository, EncryptionPort encryptionPort) {
        this.notificationRepository = notificationRepository;
        this.encryptionPort = encryptionPort;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public NotificationIdView execute(AcceptNotificationCmd cmd) {
        List<Notification> existing =
                notificationRepository.findBy(NotificationCriteria.byIdempotencyKey(cmd.idempotencyKey()));
        if (!existing.isEmpty()) {
            Notification n = existing.get(0);
            return new NotificationIdView(n.id().value(), n.status().name());
        }

        Map<String, String> encrypted = new LinkedHashMap<>();
        if (cmd.variables() != null) {
            cmd.variables().forEach((k, v) -> encrypted.put(k, encryptionPort.encrypt(v)));
        }

        Notification notification = Notification.accept(
                new NotificationId(UUID.randomUUID().toString()), cmd.idempotencyKey(), cmd.userId(),
                Channel.valueOf(cmd.channel().trim().toUpperCase()), cmd.templateId(), encrypted,
                Priority.valueOf(cmd.priority().trim().toUpperCase()), cmd.sourceEventType(), cmd.sourceEventId());

        notificationRepository.save(notification);
        return new NotificationIdView(notification.id().value(), notification.status().name());
    }
}
