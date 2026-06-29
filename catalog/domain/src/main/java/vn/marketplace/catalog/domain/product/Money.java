package vn.marketplace.catalog.domain.product;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * Money held as an integer amount in the currency's minor unit (VND đồng is itself the minor unit),
 * stored as a {@code long} — avoids floating-point drift and matches the {@code bigint price} column
 * in the tech-spec data model.
 *
 * <p>{@code amount} is non-negative here; the strictly-positive price rule (INVALID_PRICE) is a SKU
 * invariant enforced in {@link vn.marketplace.catalog.domain.product.management.Sku}.
 */
public record Money(long amount, Currency currency) implements DomainValue {

    public Money {
        Objects.requireNonNull(currency, "currency cannot be null");
        if (amount < 0) {
            throw new InvalidArgumentException("amount cannot be negative");
        }
    }

    public static Money of(long amount, Currency currency) {
        return new Money(amount, currency);
    }
}
