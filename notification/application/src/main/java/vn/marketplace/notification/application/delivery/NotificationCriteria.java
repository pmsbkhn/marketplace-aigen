package vn.marketplace.notification.application.delivery;

import tech.vsf.ptnt.msfw.domain.core.Criteria;

/** Lookup criteria for the {@code Repository<Notification>} port — {@code idempotencyKey} for dedup. */
public record NotificationCriteria(String idempotencyKey) implements Criteria {

    public static NotificationCriteria byIdempotencyKey(String idempotencyKey) {
        return new NotificationCriteria(idempotencyKey);
    }
}
