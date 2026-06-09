package vn.marketplace.inventory.domain.stock;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/** Identity of a single {@link vn.marketplace.inventory.domain.stock.management.Reservation} (ULID/UUID). */
public class ReservationId extends Identity<String> {
    private final String value;

    public ReservationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReservationId cannot be null or blank");
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
        return Objects.equals(value, ((ReservationId) o).value);
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
