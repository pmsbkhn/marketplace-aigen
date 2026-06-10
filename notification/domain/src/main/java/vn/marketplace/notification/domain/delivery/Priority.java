package vn.marketplace.notification.domain.delivery;

/**
 * Notification priority. {@code TRANSACTIONAL} (OTP, order updates) bypasses opt-out (fail-open);
 * {@code BULK} (marketing) MUST honour the user's opt-out preference (fail-closed → SUPPRESSED).
 */
public enum Priority {
    TRANSACTIONAL,
    BULK
}
