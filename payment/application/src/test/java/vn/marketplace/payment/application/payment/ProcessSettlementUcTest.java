package vn.marketplace.payment.application.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.application.payment.PaymentTestFakes.FakeWormDocStore;
import vn.marketplace.payment.application.payment.ProcessSettlementCmd.SettlementItemInput;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.EscrowStatus;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.management.CommissionPolicy;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

class ProcessSettlementUcTest {

    private InMemoryPaymentRepository payments;
    private InMemorySettlementRepository settlements;
    private FakeWormDocStore docStore;
    private ProcessSettlementUc uc;

    @BeforeEach
    void setUp() {
        payments = new InMemoryPaymentRepository();
        settlements = new InMemorySettlementRepository();
        docStore = new FakeWormDocStore();
        uc = new ProcessSettlementUc(payments, settlements, docStore, CommissionPolicy.standard());
        DomainEventPublisher.clear();
    }

    @AfterEach
    void tearDown() {
        DomainEventPublisher.clear();
    }

    private Payment paidPayment(long amountForOrder1) {
        Payment payment = Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1",
                Money.of(amountForOrder1, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", amountForOrder1)));
        payment.confirmPayment("TXN-1", amountForOrder1);
        payments.save(payment);
        DomainEventPublisher.clear();
        return payment;
    }

    private ProcessSettlementCmd cmd() {
        return new ProcessSettlementCmd("O-1", "M-1",
                List.of(new SettlementItemInput("SKU-1", 500_000, 2)),
                "9704-1234-5678-9012");
    }

    @Test
    void settlesCompletedOrderWithCommissionDocAndReleasedEscrow() {
        paidPayment(1_000_000);

        SettlementView view = uc.execute(cmd());

        assertEquals("COMPLETED", view.status());
        assertEquals(1_000_000, view.grossAmount());
        assertEquals(20_000, view.commission());
        assertEquals(980_000, view.netAmount());
        assertEquals("s3://settlement-docs/settlements/O-1.json", view.docUri());
        assertEquals("PENDING", view.payoutStatus(), "payout submits in ProcessPayout, not here");
        assertEquals(EscrowStatus.RELEASED, payments.store.get("PAY-1").holds().get(0).status());
    }

    @Test
    void settlementIsIdempotentByOrderId() {
        paidPayment(1_000_000);

        SettlementView first = uc.execute(cmd());
        SettlementView second = uc.execute(cmd());

        assertEquals(first.settlementId(), second.settlementId());
        assertEquals(1, settlements.store.size(), "one settlement per order");
        assertEquals(1, docStore.writes, "WORM document written once");
    }

    @Test
    void unpaidPaymentVetoesSettlement() { // TC-PAY-03 at the use-case level
        Payment pending = Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1",
                Money.of(1_000_000, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", 1_000_000)));
        payments.save(pending);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class, () -> uc.execute(cmd()));

        assertEquals(PaymentErrorCode.PAYMENT_NOT_COMPLETED, ex.paymentErrorCode());
        assertEquals(0, settlements.store.size());
        assertEquals(0, docStore.writes);
    }

    @Test
    void orderTotalDriftIsVetoed() {
        paidPayment(1_000_000);
        ProcessSettlementCmd drifted = new ProcessSettlementCmd("O-1", "M-1",
                List.of(new SettlementItemInput("SKU-1", 400_000, 2)), // 800k ≠ escrowed 1M
                "9704-1234-5678-9012");

        PaymentDomainException ex = assertThrows(PaymentDomainException.class, () -> uc.execute(drifted));

        assertEquals(PaymentErrorCode.AMOUNT_MISMATCH, ex.paymentErrorCode());
        assertEquals(0, settlements.store.size());
    }

    @Test
    void unknownOrderThrowsNotFound() {
        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> uc.execute(new ProcessSettlementCmd("O-404", "M-1", List.of(), "9704")));
        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, ex.paymentErrorCode());
    }
}
