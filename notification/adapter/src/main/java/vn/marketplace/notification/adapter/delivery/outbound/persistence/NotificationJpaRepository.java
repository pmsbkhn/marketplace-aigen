package vn.marketplace.notification.adapter.delivery.outbound.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.marketplace.notification.adapter.delivery.outbound.persistence.entity.NotificationEntity;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    Optional<NotificationEntity> findByNotificationId(String notificationId);

    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);

    void deleteByNotificationId(String notificationId);
}
