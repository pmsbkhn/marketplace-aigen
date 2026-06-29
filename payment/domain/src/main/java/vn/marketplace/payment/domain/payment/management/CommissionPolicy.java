package vn.marketplace.payment.domain.payment.management;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

import vn.marketplace.payment.domain.payment.Money;

/**
 * Domain service computing the platform commission for a settlement. Rate is expressed in basis
 * points (1 bp = 0.01%) so the arithmetic stays in exact integers — the standard merchant tier is
 * 2% (TC-PAY-05: gross 1.000.000 → commission 20.000, net 980.000). Tiered rates (per merchant
 * ranking) plug in by constructing with a different rate.
 */
public class CommissionPolicy {

    /** Standard merchant tier: 2%. */
    public static final int STANDARD_RATE_BASIS_POINTS = 200;

    private final int rateBasisPoints;

    public CommissionPolicy(int rateBasisPoints) {
        if (rateBasisPoints < 0 || rateBasisPoints > 10_000) {
            throw new InvalidArgumentException("rateBasisPoints must be between 0 and 10000");
        }
        this.rateBasisPoints = rateBasisPoints;
    }

    public static CommissionPolicy standard() {
        return new CommissionPolicy(STANDARD_RATE_BASIS_POINTS);
    }

    public Money commissionFor(Money gross) {
        return gross.percentBasisPoints(rateBasisPoints);
    }

    public Money netFor(Money gross) {
        return gross.minus(commissionFor(gross));
    }

    public int rateBasisPoints() {
        return rateBasisPoints;
    }
}
