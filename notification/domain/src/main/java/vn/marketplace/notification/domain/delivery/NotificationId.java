package vn.marketplace.notification.domain.delivery;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/** Notification aggregate identity (ULID/UUID). */
public class NotificationId extends Identity<String> {
    private final String value;

    public NotificationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NotificationId cannot be null or blank");
        }
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(value, ((NotificationId) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
