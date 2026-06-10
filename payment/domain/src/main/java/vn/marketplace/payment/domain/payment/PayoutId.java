package vn.marketplace.payment.domain.payment;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/** Identity of the payout instruction inside the Settlement aggregate. */
public class PayoutId extends Identity<String> {
    private final String value;

    public PayoutId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PayoutId cannot be null or blank");
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
        return Objects.equals(value, ((PayoutId) o).value);
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
