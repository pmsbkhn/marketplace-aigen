package vn.marketplace.notification.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainErrorCode;

/**
 * Notification-specific business error codes. Each maps onto a coarse msfw {@link DomainErrorCode}
 * (so the GlobalExceptionHandler picks the HTTP status) while carrying the precise business meaning
 * the tech-spec/tests assert on.
 */
public enum NotificationErrorCode {
    MISSING_TEMPLATE(DomainErrorCode.INVALID_ARGUMENT, "A template id is required"),
    INVALID_TRANSITION(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Illegal notification status transition"),
    MAX_ATTEMPTS_EXCEEDED(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Maximum delivery attempts exceeded"),
    NOTIFICATION_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Notification not found");

    private final DomainErrorCode baseErrorCode;
    private final String defaultMessage;

    NotificationErrorCode(DomainErrorCode baseErrorCode, String defaultMessage) {
        this.baseErrorCode = baseErrorCode;
        this.defaultMessage = defaultMessage;
    }

    public DomainErrorCode baseErrorCode() {
        return baseErrorCode;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
