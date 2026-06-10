package vn.marketplace.payment.domain.payment.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PayoutId;
import vn.marketplace.payment.domain.payment.PayoutStatus;
import vn.marketplace.payment.domain.payment.SettlementId;
import vn.marketplace.payment.domain.payment.SettlementStatus;
import vn.marketplace.payment.domain.shared.InvalidTransitionException;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

class SettlementTest {

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Settlement processingSettlement(long gross) {
        return Settlement.start(new SettlementId("SET-1"), "O-1", new MerchantId("M-1"),
                Money.of(gross, Currency.VND), CommissionPolicy.standard(),
                new PayoutId("PO-1"), "9704-1234-5678-9012");
    }

    @Test
    void completeWithoutDocumentIsVetoed() { // TC-PAY-04
        Settlement settlement = processingSettlement(1_000_000);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class, settlement::complete);

        assertEquals(PaymentErrorCode.MISSING_SETTLEMENT_DOCUMENT, ex.paymentErrorCode());
        assertEquals(SettlementStatus.PROCESSING, settlement.status());
    }

    @Test
    void completeWithDocumentSucceedsAndIsTerminal() {
        Settlement settlement = processingSettlement(1_000_000);
        settlement.attachDocument("s3://settlement-docs/SET-1.json");

        settlement.complete();
        assertEquals(SettlementStatus.COMPLETED, settlement.status());

        assertThrows(InvalidTransitionException.class, settlement::complete);
    }

    @Test
    void payoutSubmitTwiceIsNoOp() { // TC-PAY-06
        Settlement settlement = processingSettlement(1_000_000);

        assertTrue(settlement.submitPayout(), "first submit goes through");
        assertEquals(PayoutStatus.SUBMITTED, settlement.payout().status());
        assertEquals(1, DomainEventPublisher.getEvents().size());

        DomainEventPublisher.clear();
        assertFalse(settlement.submitPayout(), "second submit is blocked (no duplicate bank order)");
        assertTrue(DomainEventPublisher.getEvents().isEmpty(), "no duplicate PayoutCompleted");
    }

    @Test
    void payoutSubmitPublishesPayoutCompletedWithMaskedAccount() {
        Settlement settlement = processingSettlement(1_000_000);

        settlement.submitPayout();

        PayoutCompleted event = assertInstanceOf(PayoutCompleted.class, DomainEventPublisher.getEvents().get(0));
        assertEquals("PO-1", event.payoutId());
        assertEquals("M-1", event.merchantId());
        assertEquals(980_000, event.amount());
        assertEquals("9012", event.bankLast4());
    }

    @Test
    void settlementCarriesCommissionArithmetic() {
        Settlement settlement = processingSettlement(1_000_000);

        assertEquals(1_000_000, settlement.grossAmount().amount());
        assertEquals(20_000, settlement.commission().amount());
        assertEquals(980_000, settlement.netAmount().amount());
        assertEquals(980_000, settlement.payout().amount().amount());
    }

    @Test
    void payoutRequiresBankAccount() {
        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> Settlement.start(new SettlementId("SET-1"), "O-1", new MerchantId("M-1"),
                        Money.of(1_000_000, Currency.VND), CommissionPolicy.standard(),
                        new PayoutId("PO-1"), " "));
        assertEquals(PaymentErrorCode.BANK_ACCOUNT_REQUIRED, ex.paymentErrorCode());
    }

    @Test
    void attachingBlankDocumentIsRejected() {
        Settlement settlement = processingSettlement(1_000_000);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> settlement.attachDocument(" "));
        assertEquals(PaymentErrorCode.MISSING_SETTLEMENT_DOCUMENT, ex.paymentErrorCode());
    }
}
