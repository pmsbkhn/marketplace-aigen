package vn.marketplace.payment.domain.payment.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.Money;

class CommissionPolicyTest {

    @Test
    void standardTierTakesTwoPercent() { // TC-PAY-05: gross 1.000.000 → commission 20.000, net 980.000
        CommissionPolicy policy = CommissionPolicy.standard();
        Money gross = Money.of(1_000_000, Currency.VND);

        assertEquals(20_000, policy.commissionFor(gross).amount());
        assertEquals(980_000, policy.netFor(gross).amount());
    }

    @Test
    void commissionIsFloorRoundedExactIntegerArithmetic() {
        CommissionPolicy policy = CommissionPolicy.standard();

        // 2% of 99 đồng = 1.98 → floor 1; net 98 (no floating point drift)
        assertEquals(1, policy.commissionFor(Money.of(99, Currency.VND)).amount());
        assertEquals(98, policy.netFor(Money.of(99, Currency.VND)).amount());
    }

    @Test
    void customTierRateApplies() {
        CommissionPolicy fivePercent = new CommissionPolicy(500);

        assertEquals(50_000, fivePercent.commissionFor(Money.of(1_000_000, Currency.VND)).amount());
    }

    @Test
    void rateOutsideRangeIsRejected() {
        assertThrows(InvalidArgumentException.class, () -> new CommissionPolicy(-1));
        assertThrows(InvalidArgumentException.class, () -> new CommissionPolicy(10_001));
    }
}
