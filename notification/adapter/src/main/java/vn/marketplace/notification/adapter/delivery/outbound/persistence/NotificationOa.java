package vn.marketplace.notification.adapter.delivery.outbound.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.springcore.persistence.AbstractMementoJpaOa;
import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.notification.adapter.delivery.outbound.persistence.entity.DeliveryAttemptEntity;
import vn.marketplace.notification.adapter.delivery.outbound.persistence.entity.NotificationEntity;
import vn.marketplace.notification.domain.delivery.management.Notification;

/**
 * Outbound persistence adapter: maps the {@link Notification} aggregate ↔ JPA entities at the
 * {@link Notification.Memento} level via msfw's {@link AbstractMementoJpaOa} (save/find/delete/paging
 * plumbing lives in the base class). Attempts are append-only — the memento is the source of truth, so
 * on update the merged collection replaces the rows (orphanRemoval syncs the table), exactly as before.
 * {@code @Transactional} keeps the session open while the lazy attempts/variables collections are read.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class NotificationOa extends AbstractMementoJpaOa<Notification, Notification.Memento, NotificationEntity> {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    protected JpaOaRepository<NotificationEntity> jpa() {
        return notificationJpaRepository;
    }

    @Override
    protected NotificationEntity fromMemento(Notification.Memento m) {
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(m.notificationId());
        entity.setIdempotencyKey(m.idempotencyKey());
        entity.setUserId(m.userId());
        entity.setChannel(m.channel());
        entity.setTemplateId(m.templateId());
        entity.setPriority(m.priority());
        entity.setStatus(m.status());
        entity.setSourceEventType(m.sourceEventType());
        entity.setSourceEventId(m.sourceEventId());
        entity.setCreatedAt(m.createdAt());
        entity.setUpdatedAt(m.updatedAt());

        // Variables are immutable once accepted — same ciphertext values are (re)written on update.
        entity.getVariables().putAll(m.variables());

        for (Notification.AttemptMemento am : m.attempts()) {
            DeliveryAttemptEntity ae = new DeliveryAttemptEntity();
            ae.setAttemptNo(am.attemptNo());
            ae.setProvider(am.provider());
            ae.setResult(am.result());
            ae.setAttemptedAt(am.attemptedAt());
            entity.getAttempts().add(ae);
        }
        return entity;
    }

    @Override
    protected Notification.Memento toMemento(NotificationEntity e) {
        List<Notification.AttemptMemento> attempts = new ArrayList<>();
        for (DeliveryAttemptEntity ae : e.getAttempts()) {
            attempts.add(new Notification.AttemptMemento(ae.getAttemptNo(), ae.getProvider(),
                    ae.getResult(), ae.getAttemptedAt()));
        }
        return new Notification.Memento(e.getId(), e.getNotificationId(), e.getIdempotencyKey(),
                e.getUserId(), e.getChannel(), e.getTemplateId(), new LinkedHashMap<>(e.getVariables()),
                e.getPriority(), e.getStatus(), e.getSourceEventType(), e.getSourceEventId(),
                e.getCreatedAt(), e.getUpdatedAt(), attempts);
    }

    @Override
    protected Notification restore(Notification.Memento memento) {
        return Notification.restore(memento);
    }

    @Override
    protected Criteria identityCriteria(Identity<?> id) {
        return Criteria.where("notificationId").eq(id.value());
    }
}
