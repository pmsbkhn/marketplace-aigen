package vn.marketplace.catalog.domain.product;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

public class VariantId extends Identity<String> {
    private final String value;

    public VariantId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VariantId cannot be null or blank");
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
        return Objects.equals(value, ((VariantId) o).value);
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
