package vn.marketplace.catalog.domain.product;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

public class SkuId extends Identity<String> {
    private final String value;

    public SkuId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SkuId cannot be null or blank");
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
        return Objects.equals(value, ((SkuId) o).value);
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
