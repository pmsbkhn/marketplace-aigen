package vn.marketplace.catalog.domain.product;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

/**
 * Supported currencies. Marketplace prices are quoted in minor units (e.g. VND đồng) — see {@link Money}.
 */
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
