package vn.marketplace.payment.domain.payment;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * Money as an integer amount in the currency's minor unit (VND đồng is itself the minor unit), stored
 * as a {@code long} to avoid floating-point drift — real-money arithmetic must be exact. Non-negative;
 * strictly-positive rules live at the aggregate boundary.
 */
public record Money(long amount, Currency currency) implements DomainValue {

    public Money {
        Objects.requireNonNull(currency, "currency cannot be null");
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
    }

    public static Money of(long amount, Currency currency) {
        return new Money(amount, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount - other.amount, this.currency);
    }

    /** Integer percentage taken in basis points (1 bp = 0.01%), floor-rounded — deterministic fees. */
    public Money percentBasisPoints(int basisPoints) {
        if (basisPoints < 0 || basisPoints > 10_000) {
            throw new IllegalArgumentException("basisPoints must be between 0 and 10000");
        }
        return new Money(this.amount * basisPoints / 10_000, this.currency);
    }

    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("cannot combine money of different currencies");
        }
    }
}
