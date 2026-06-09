package vn.marketplace.catalog.domain.product;

/**
 * Supported currencies. Marketplace prices are quoted in minor units (e.g. VND đồng) — see {@link Money}.
 */
public enum Currency {
    VND,
    USD,
    EUR;

    public static Currency of(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be null or blank");
        }
        try {
            return Currency.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported currency code: " + code);
        }
    }
}
