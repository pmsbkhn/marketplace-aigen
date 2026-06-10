package vn.marketplace.notification.adapter.delivery.outbound.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.notification.adapter.delivery.outbound.persistence.entity.DeliveryAttemptEntity;
import vn.marketplace.notification.adapter.delivery.outbound.persistence.entity.NotificationEntity;
import vn.marketplace.notification.application.delivery.NotificationCriteria;
import vn.marketplace.notification.domain.delivery.management.Notification;

/**
 * Outbound persistence adapter: maps the {@link Notification} aggregate ↔ JPA entities via the
 * aggregate {@link Notification.Memento}, implementing the msfw {@code Repository<Notification>} port.
 * {@code @Transactional} keeps the session open while the lazy attempts/variables collections are read.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class NotificationOa implements tech.vsf.ptnt.msfw.domain.core.Repository<Notification> {

    private final NotificationJpaRepository jpa;

    @Override
    public void save(Notification aggregate) {
        Notification.Memento m = aggregate.toMemento();
        NotificationEntity entity = jpa.findByNotificationId(m.notificationId()).orElseGet(NotificationEntity::new);
        boolean isNew = entity.getId() == null;

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

        if (isNew) { // variables are immutable once accepted
            entity.getVariables().putAll(m.variables());
        }

        // Attempts are append-only — rebuild from the memento (orphanRemoval syncs the table).
        entity.getAttempts().clear();
        for (Notification.AttemptMemento am : m.attempts()) {
            DeliveryAttemptEntity ae = new DeliveryAttemptEntity();
            ae.setAttemptNo(am.attemptNo());
            ae.setProvider(am.provider());
            ae.setResult(am.result());
            ae.setAttemptedAt(am.attemptedAt());
            entity.getAttempts().add(ae);
        }

        NotificationEntity saved = jpa.save(entity);
        aggregate.set_id(saved.getId());
    }

    @Override
    public <U extends Identity<?>> Optional<Notification> findById(U id) {
        return jpa.findByNotificationId(String.valueOf(id.value())).map(this::toDomain);
    }

    @Override
    public List<Notification> findBy(Criteria criteria) {
        NotificationCriteria c = (NotificationCriteria) criteria;
        if (c.idempotencyKey() != null) {
            return jpa.findByIdempotencyKey(c.idempotencyKey()).map(this::toDomain).map(List::of).orElseGet(List::of);
        }
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public PagedSearchResult<Notification> findBy(Criteria criteria, Pagination pagination) {
        List<Notification> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Notification> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        jpa.deleteByNotificationId(String.valueOf(id.value()));
    }

    private Notification toDomain(NotificationEntity e) {
        List<Notification.AttemptMemento> attempts = new ArrayList<>();
        for (DeliveryAttemptEntity ae : e.getAttempts()) {
            attempts.add(new Notification.AttemptMemento(ae.getAttemptNo(), ae.getProvider(),
                    ae.getResult(), ae.getAttemptedAt()));
        }
        Notification.Memento m = new Notification.Memento(e.getId(), e.getNotificationId(), e.getIdempotencyKey(),
                e.getUserId(), e.getChannel(), e.getTemplateId(), new java.util.LinkedHashMap<>(e.getVariables()),
                e.getPriority(), e.getStatus(), e.getSourceEventType(), e.getSourceEventId(),
                e.getCreatedAt(), e.getUpdatedAt(), attempts);
        return Notification.fromMemento(m);
    }
}
