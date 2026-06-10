package vn.marketplace.notification.domain.delivery;

/**
 * Notification lifecycle — one-directional, terminal states immutable:
 * {@code ACCEPTED → RENDERED → (SENT | SUPPRESSED | FAILED)}. {@code SUPPRESSED} is also reachable
 * directly from ACCEPTED (opt-out, before rendering). {@code SENT}/{@code SUPPRESSED}/{@code FAILED}
 * are terminal.
 */
public enum NotificationStatus {
    ACCEPTED,
    RENDERED,
    SENT,
    SUPPRESSED,
    FAILED
}
