package vn.marketplace.checkout.domain.checkout;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/**
 * The client-supplied idempotency key — identity of one checkout saga AND the {@code orderRef}
 * propagated to Inventory (reserve) and Payment (escrow). Immutable once the session exists.
 */
public class IdempotencyKey extends Identity<String> {
    private final String value;

    public IdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey cannot be null or blank");
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
        return Objects.equals(value, ((IdempotencyKey) o).value);
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
