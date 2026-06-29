package vn.marketplace.checkout.domain.checkout;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * Money as an integer amount in the currency's minor unit (VND đồng is itself the minor unit),
 * stored as a {@code long} to avoid floating-point drift in cart totals.
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

    public Money plus(Money other) {
        if (this.currency != other.currency) {
            throw new InvalidArgumentException("cannot add money of different currencies");
        }
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money times(int quantity) {
        return new Money(this.amount * quantity, this.currency);
    }
}
