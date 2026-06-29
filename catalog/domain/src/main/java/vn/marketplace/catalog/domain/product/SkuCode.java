package vn.marketplace.catalog.domain.product;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

import java.util.Objects;
import java.util.regex.Pattern;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * Business SKU code — UNIQUE across the whole marketplace (enforced by a DB constraint at the
 * persistence boundary). Alphanumeric plus dash/underscore.
 */
public record SkuCode(String value) implements DomainValue {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$");

    public SkuCode {
        Objects.requireNonNull(value, "sku code cannot be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException("Invalid sku code: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
