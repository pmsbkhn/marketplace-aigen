package vn.marketplace.inventory.domain.stock;

import java.util.Objects;
import java.util.regex.Pattern;

import tech.vsf.ptnt.msfw.domain.core.Identity;

/**
 * Business SKU code — the {@link vn.marketplace.inventory.domain.stock.management.Stock} aggregate
 * identity. UNIQUE marketplace-wide (enforced by a DB constraint at the persistence boundary).
 * Alphanumeric plus dash/underscore — same shape as Catalog's SkuCode.
 */
public class SkuCode extends Identity<String> {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$");

    private final String value;

    public SkuCode(String value) {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid sku code: " + value);
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
        return Objects.equals(value, ((SkuCode) o).value);
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
