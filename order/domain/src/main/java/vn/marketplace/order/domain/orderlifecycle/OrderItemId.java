package vn.marketplace.order.domain.orderlifecycle;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/** Identity of an {@code OrderItem} (ULID/UUID). */
public class OrderItemId extends Identity<String> {
    private final String value;

    public OrderItemId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderItemId cannot be null or blank");
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
        return Objects.equals(value, ((OrderItemId) o).value);
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
