package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

/** Supported currencies. Order amounts are quoted in minor units (VND đồng) — see {@link Money}. */
public enum Currency {
    VND,
    USD,
    EUR;

    public static Currency of(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidArgumentException("Currency code cannot be null or blank");
        }
        try {
            return Currency.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidArgumentException("Unsupported currency code: " + code);
        }
    }
}
