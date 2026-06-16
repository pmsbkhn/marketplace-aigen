package vn.marketplace.notification.domain.delivery;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Notification aggregate identity (ULID/UUID). */
public class NotificationId extends StringIdentity {

    public NotificationId(String value) {
        super(value);
    }
}
