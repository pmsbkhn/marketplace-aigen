package vn.marketplace.inventory.domain.stock;

import java.util.regex.Pattern;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;
import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

/**
 * Business SKU code — the {@link vn.marketplace.inventory.domain.stock.management.Stock} aggregate
 * identity. UNIQUE marketplace-wide (enforced by a DB constraint at the persistence boundary).
 * Alphanumeric plus dash/underscore — same shape as Catalog's SkuCode.
 */
public class SkuCode extends StringIdentity {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$");

    public SkuCode(String value) {
        super(value);
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException("Invalid sku code: " + value);
        }
    }
}
