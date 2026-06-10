package vn.marketplace.notification.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainException;

/**
 * Domain exception for the Notification context, parameterised by a {@link NotificationErrorCode}.
 * Extends msfw's abstract {@link DomainException} so the GlobalExceptionHandler maps it to the right
 * HTTP status; the precise business reason is carried by {@link #notificationErrorCode()}.
 */
public class NotificationDomainException extends DomainException {

    private final NotificationErrorCode notificationErrorCode;

    public NotificationDomainException(NotificationErrorCode code) {
        super(code.baseErrorCode(), code.defaultMessage());
        this.notificationErrorCode = code;
    }

    public NotificationDomainException(NotificationErrorCode code, String message) {
        super(code.baseErrorCode(), message);
        this.notificationErrorCode = code;
    }

    public NotificationErrorCode notificationErrorCode() {
        return notificationErrorCode;
    }
}
