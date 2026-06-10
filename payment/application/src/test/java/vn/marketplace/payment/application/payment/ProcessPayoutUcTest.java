package vn.marketplace.payment.application.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.application.payment.PaymentTestFakes.FakeBank;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PayoutId;
import vn.marketplace.payment.domain.payment.PayoutStatus;
import vn.marketplace.payment.domain.payment.SettlementId;
import vn.marketplace.payment.domain.payment.management.CommissionPolicy;
import vn.marketplace.payment.domain.payment.management.PayoutCompleted;
import vn.marketplace.payment.domain.payment.management.Settlement;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

class ProcessPayoutUcTest {

    private InMemorySettlementRepository settlements;
    private FakeBank bank;
    private ProcessPayoutUc uc;

    @BeforeEach
    void setUp() {
        settlements = new InMemorySettlementRepository();
        bank = new FakeBank();
        uc = new ProcessPayoutUc(settlements, bank);
        DomainEventPublisher.clear();
    }

    @AfterEach
    void tearDown() {
        DomainEventPublisher.clear();
    }

    private Settlement completedSettlement() {
        Settlement settlement = Settlement.start(new SettlementId("SET-1"), "O-1", new MerchantId("M-1"),
                Money.of(1_000_000, Currency.VND), CommissionPolicy.standard(),
                new PayoutId("PO-1"), "9704-1234-5678-9012");
        settlement.attachDocument("s3://settlement-docs/settlements/O-1.json");
        settlement.complete();
        settlements.save(settlement);
        DomainEventPublisher.clear();
        return settlement;
    }

    @Test
    void submitsNetPayoutToBankAndPublishesPayoutCompleted() {
        completedSettlement();

        SettlementView view = uc.execute(new ProcessPayoutCmd("O-1"));

        assertEquals("SUBMITTED", view.payoutStatus());
        assertEquals(1, bank.instructions.size());
        assertEquals(980_000, bank.instructions.get(0).amount());
        assertEquals("9704-1234-5678-9012", bank.instructions.get(0).bankAccount());
        assertInstanceOf(PayoutCompleted.class, DomainEventPublisher.getEvents().get(0));
    }

    @Test
    void secondPayoutIsNoOp_noDuplicateBankInstruction() { // TC-PAY-06 at the use-case level
        completedSettlement();
        uc.execute(new ProcessPayoutCmd("O-1"));
        DomainEventPublisher.clear();

        SettlementView second = uc.execute(new ProcessPayoutCmd("O-1"));

        assertEquals("SUBMITTED", second.payoutStatus());
        assertEquals(1, bank.instructions.size(), "exactly one bank instruction, ever");
        assertTrue(DomainEventPublisher.getEvents().isEmpty(), "no duplicate PayoutCompleted");
    }

    @Test
    void bankFailureLeavesPayoutPendingForSafeRetry() {
        Settlement settlement = completedSettlement();
        bank.toThrow = new IllegalStateException("bank API 503");

        assertThrows(IllegalStateException.class, () -> uc.execute(new ProcessPayoutCmd("O-1")));

        assertEquals(PayoutStatus.PENDING, settlement.payout().status(), "state untouched → retry safe");
        assertTrue(DomainEventPublisher.getEvents().isEmpty());
    }

    @Test
    void unknownOrderThrowsSettlementNotFound() {
        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> uc.execute(new ProcessPayoutCmd("O-404")));
        assertEquals(PaymentErrorCode.SETTLEMENT_NOT_FOUND, ex.paymentErrorCode());
    }
}
