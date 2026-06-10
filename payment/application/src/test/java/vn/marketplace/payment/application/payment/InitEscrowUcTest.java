package vn.marketplace.payment.application.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.application.payment.InitEscrowCmd.EscrowAllocationInput;
import vn.marketplace.payment.application.payment.PaymentTestFakes.FakeGateway;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

class InitEscrowUcTest {

    private InMemoryPaymentRepository payments;
    private FakeGateway gateway;
    private InitEscrowUc uc;

    @BeforeEach
    void setUp() {
        payments = new InMemoryPaymentRepository();
        gateway = new FakeGateway();
        uc = new InitEscrowUc(payments, gateway);
        DomainEventPublisher.clear();
    }

    @AfterEach
    void tearDown() {
        DomainEventPublisher.clear();
    }

    private InitEscrowCmd cmd(String orderRef) {
        return new InitEscrowCmd(orderRef, 300, "VND",
                List.of(new EscrowAllocationInput("O-1", "M-1", 100),
                        new EscrowAllocationInput("O-2", "M-2", 200)));
    }

    @Test
    void initCreatesPendingPaymentWithGatewayUrl() {
        InitEscrowResult result = uc.execute(cmd("CHK-1"));

        assertNotNull(result.escrowId());
        assertEquals("https://pg.example.com/pay/" + result.escrowId(), result.paymentUrl());
        assertNotNull(result.expiresAt());
        assertEquals(1, payments.store.size());
        assertEquals("PENDING", payments.store.values().iterator().next().status().name());
    }

    @Test
    void duplicateOrderRefReturnsExistingEscrowWithoutSecondGatewayCall() {
        InitEscrowResult first = uc.execute(cmd("CHK-1"));
        InitEscrowResult second = uc.execute(cmd("CHK-1"));

        assertEquals(first.escrowId(), second.escrowId());
        assertEquals(first.paymentUrl(), second.paymentUrl());
        assertEquals(1, payments.store.size(), "no second payment");
        assertEquals(1, gateway.createdFor.size(), "no second gateway transaction");
    }

    @Test
    void allocationSumMismatchIsRejectedAndNothingPersisted() {
        InitEscrowCmd bad = new InitEscrowCmd("CHK-1", 999, "VND",
                List.of(new EscrowAllocationInput("O-1", "M-1", 100)));

        PaymentDomainException ex = assertThrows(PaymentDomainException.class, () -> uc.execute(bad));

        assertEquals(PaymentErrorCode.ALLOCATION_SUM_MISMATCH, ex.paymentErrorCode());
        assertTrue(payments.store.isEmpty());
        assertTrue(gateway.createdFor.isEmpty());
    }
}
