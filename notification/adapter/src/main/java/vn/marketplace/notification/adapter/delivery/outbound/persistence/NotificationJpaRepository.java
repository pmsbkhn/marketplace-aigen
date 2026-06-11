package vn.marketplace.notification.adapter.delivery.outbound.persistence;

import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.notification.adapter.delivery.outbound.persistence.entity.NotificationEntity;

/**
 * All lookups (notificationId identity, idempotencyKey dedup) are root-attribute filters expressed
 * through the msfw {@code Criteria} DSL, so no derived finder methods are needed.
 */
public interface NotificationJpaRepository extends JpaOaRepository<NotificationEntity> {
}
