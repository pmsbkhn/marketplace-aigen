package vn.marketplace.payment.application.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.PaymentStatus;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.payment.management.PaymentFailed;
import vn.marketplace.payment.domain.payment.management.PaymentReceived;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

class HandleWebhookUcTest {

    private InMemoryPaymentRepository payments;
    private HandleWebhookUc uc;

    @BeforeEach
    void setUp() {
        payments = new InMemoryPaymentRepository();
        uc = new HandleWebhookUc(payments);
        DomainEventPublisher.clear();
    }

    @AfterEach
    void tearDown() {
        DomainEventPublisher.clear();
    }

    private Payment pendingPayment(String orderRef, long amount) {
        Payment payment = Payment.initEscrow(new PaymentId("PAY-" + orderRef), orderRef,
                Money.of(amount, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", amount)));
        payments.save(payment);
        DomainEventPublisher.clear();
        return payment;
    }

    @Test
    void successWebhookConfirmsPaymentAndPublishesPaymentReceived() {
        pendingPayment("CHK-1", 1_500_000);

        uc.execute(new HandleWebhookCmd("TXN-1", "CHK-1", 1_500_000, "SUCCESS"));

        Payment payment = payments.store.get("PAY-CHK-1");
        assertEquals(PaymentStatus.PAID, payment.status());
        assertEquals("TXN-1", payment.gatewayTxnId());
        assertInstanceOf(PaymentReceived.class, DomainEventPublisher.getEvents().get(0));
    }

    @Test
    void tamperedAmountIsVetoedAndStaysPending() { // TC-PAY-01 at the use-case level
        pendingPayment("CHK-1", 1_500_000);

        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> uc.execute(new HandleWebhookCmd("TXN-1", "CHK-1", 1_000_000, "SUCCESS")));

        assertEquals(PaymentErrorCode.AMOUNT_MISMATCH, ex.paymentErrorCode());
        assertEquals(PaymentStatus.PENDING, payments.store.get("PAY-CHK-1").status());
        assertTrue(DomainEventPublisher.getEvents().isEmpty());
    }

    @Test
    void failedWebhookMarksFailedAndPublishesPaymentFailed() {
        pendingPayment("CHK-1", 500);

        uc.execute(new HandleWebhookCmd("TXN-1", "CHK-1", 500, "FAILED"));

        assertEquals(PaymentStatus.FAILED, payments.store.get("PAY-CHK-1").status());
        assertInstanceOf(PaymentFailed.class, DomainEventPublisher.getEvents().get(0));
    }

    @Test
    void redeliveredWebhookIsSilentNoOp() {
        pendingPayment("CHK-1", 500);
        uc.execute(new HandleWebhookCmd("TXN-1", "CHK-1", 500, "SUCCESS"));
        DomainEventPublisher.clear();
        int savesBefore = payments.saved.size();

        uc.execute(new HandleWebhookCmd("TXN-1", "CHK-1", 500, "SUCCESS")); // at-least-once redelivery

        assertEquals(savesBefore, payments.saved.size(), "no second save");
        assertTrue(DomainEventPublisher.getEvents().isEmpty(), "no second PaymentReceived");
    }

    @Test
    void txnIdAlreadyConsumedByAnotherPaymentIsRejected() {
        pendingPayment("CHK-1", 500);
        pendingPayment("CHK-2", 700);
        uc.execute(new HandleWebhookCmd("TXN-1", "CHK-1", 500, "SUCCESS"));

        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> uc.execute(new HandleWebhookCmd("TXN-1", "CHK-2", 700, "SUCCESS")));

        assertEquals(PaymentErrorCode.DUPLICATE_GATEWAY_TXN, ex.paymentErrorCode());
        assertEquals(PaymentStatus.PENDING, payments.store.get("PAY-CHK-2").status());
    }

    @Test
    void unknownOrderRefThrowsNotFound() {
        PaymentDomainException ex = assertThrows(PaymentDomainException.class,
                () -> uc.execute(new HandleWebhookCmd("TXN-1", "CHK-404", 1, "SUCCESS")));
        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, ex.paymentErrorCode());
    }
}
