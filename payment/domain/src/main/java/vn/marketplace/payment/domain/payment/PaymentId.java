package vn.marketplace.payment.domain.payment;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/** Payment aggregate identity (ULID/UUID) — also the escrow id returned to Checkout. */
public class PaymentId extends Identity<String> {
    private final String value;

    public PaymentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentId cannot be null or blank");
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
        return Objects.equals(value, ((PaymentId) o).value);
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
