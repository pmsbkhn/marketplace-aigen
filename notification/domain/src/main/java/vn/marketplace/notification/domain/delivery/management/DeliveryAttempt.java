package vn.marketplace.notification.domain.delivery.management;

import java.time.LocalDateTime;

import tech.vsf.ptnt.msfw.domain.core.Entity;

/**
 * One delivery attempt against a provider, inside the {@link Notification} aggregate boundary.
 * Immutable once recorded (append-only history of attempts). {@code result} is SUCCESS,
 * TRANSIENT_ERROR or PERMANENT_ERROR.
 */
public final class DeliveryAttempt implements Entity {

    private final int attemptNo;
    private final String provider;
    private final String result;
    private final LocalDateTime attemptedAt;

    DeliveryAttempt(int attemptNo, String provider, String result, LocalDateTime attemptedAt) {
        this.attemptNo = attemptNo;
        this.provider = provider;
        this.result = result;
        this.attemptedAt = attemptedAt;
    }

    static DeliveryAttempt fromMemento(Notification.AttemptMemento m) {
        return new DeliveryAttempt(m.attemptNo(), m.provider(), m.result(), m.attemptedAt());
    }

    public int attemptNo() {
        return attemptNo;
    }

    public String provider() {
        return provider;
    }

    public String result() {
        return result;
    }

    public LocalDateTime attemptedAt() {
        return attemptedAt;
    }
}
