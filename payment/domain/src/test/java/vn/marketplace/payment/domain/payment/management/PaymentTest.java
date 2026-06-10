package vn.marketplace.payment.domain.payment.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.EscrowStatus;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.PaymentStatus;
import vn.marketplace.payment.domain.shared.InvalidTransitionException;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

class PaymentTest {

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Payment pendingPayment(long amount) {
        return Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1", Money.of(amount, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", amount)));
    }

    @Test
    void webhookAmountMismatchIsVetoedAndStaysPending() { // TC-PAY-01
        Payment payment = pendingPayment(1_500_000);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> payment.confirmPayment("TXN-1", 1_000_000));

        assertEquals(PaymentErrorCode.AMOUNT_MISMATCH, ex.paymentErrorCode());
        assertEquals(PaymentStatus.PENDING, payment.status());
        assertTrue(DomainEventPublisher.getEvents().isEmpty(), "no event on a vetoed webhook");
    }

    @Test
    void paidIsTerminal_markFailedThrowsInvalidTransition() { // TC-PAY-02
        Payment payment = pendingPayment(1_500_000);
        payment.confirmPayment("TXN-1", 1_500_000);
        assertEquals(PaymentStatus.PAID, payment.status());

        assertThrows(InvalidTransitionException.class, () -> payment.markFailed("gateway timeout"));
        assertEquals(PaymentStatus.PAID, payment.status());
    }

    @Test
    void escrowReleaseRequiresPaidPayment() { // TC-PAY-03
        Payment payment = pendingPayment(1_500_000);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> payment.releaseForOrder("O-1"));

        assertEquals(PaymentErrorCode.PAYMENT_NOT_COMPLETED, ex.paymentErrorCode());
        assertEquals(EscrowStatus.HELD, payment.holds().get(0).status());
    }

    @Test
    void confirmPaymentPublishesPaymentReceivedWithAllocations() {
        Payment payment = Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1",
                Money.of(300, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", 100),
                        new Payment.NewAllocation("H-2", "O-2", "M-2", 200)));

        payment.confirmPayment("TXN-1", 300);

        assertEquals(1, DomainEventPublisher.getEvents().size());
        PaymentReceived event = assertInstanceOf(PaymentReceived.class, DomainEventPublisher.getEvents().get(0));
        assertEquals("CHK-1", event.orderRef());
        assertEquals(2, event.allocations().size());
        assertEquals("O-2", event.allocations().get(1).orderId());
    }

    @Test
    void markFailedPublishesPaymentFailedWithOrderIds() {
        Payment payment = pendingPayment(500);

        payment.markFailed("buyer abandoned");

        assertEquals(PaymentStatus.FAILED, payment.status());
        PaymentFailed event = assertInstanceOf(PaymentFailed.class, DomainEventPublisher.getEvents().get(0));
        assertEquals(List.of("O-1"), event.orderIds());
        assertEquals("buyer abandoned", event.reason());
    }

    @Test
    void releaseAfterPaidIsIdempotentPerHold() {
        Payment payment = pendingPayment(500);
        payment.confirmPayment("TXN-1", 500);

        EscrowHold first = payment.releaseForOrder("O-1");
        assertEquals(EscrowStatus.RELEASED, first.status());

        EscrowHold second = payment.releaseForOrder("O-1"); // no-op, still RELEASED
        assertEquals(EscrowStatus.RELEASED, second.status());
    }

    @Test
    void allocationsMustSumToTotal() {
        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1", Money.of(300, Currency.VND),
                        List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", 100))));
        assertEquals(PaymentErrorCode.ALLOCATION_SUM_MISMATCH, ex.paymentErrorCode());
    }

    @Test
    void escrowRequiresAtLeastOneAllocation() {
        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1", Money.of(300, Currency.VND),
                        List.of()));
        assertEquals(PaymentErrorCode.EMPTY_ALLOCATIONS, ex.paymentErrorCode());
    }

    @Test
    void failedIsTerminal_confirmPaymentThrows() {
        Payment payment = pendingPayment(500);
        payment.markFailed("expired");

        assertThrows(InvalidTransitionException.class, () -> payment.confirmPayment("TXN-1", 500));
        assertEquals(PaymentStatus.FAILED, payment.status());
    }

    @Test
    void releaseForUnknownOrderThrowsNotFound() {
        Payment payment = pendingPayment(500);
        payment.confirmPayment("TXN-1", 500);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> payment.releaseForOrder("O-999"));
        assertEquals(PaymentErrorCode.ESCROW_HOLD_NOT_FOUND, ex.paymentErrorCode());
    }
}
