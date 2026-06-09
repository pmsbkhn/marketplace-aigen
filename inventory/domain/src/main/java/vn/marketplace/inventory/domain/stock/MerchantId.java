package vn.marketplace.inventory.domain.stock;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/** Owning merchant of a {@link vn.marketplace.inventory.domain.stock.management.Stock} (tenant scope). */
public class MerchantId extends Identity<String> {
    private final String value;

    public MerchantId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MerchantId cannot be null or blank");
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
        return Objects.equals(value, ((MerchantId) o).value);
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
