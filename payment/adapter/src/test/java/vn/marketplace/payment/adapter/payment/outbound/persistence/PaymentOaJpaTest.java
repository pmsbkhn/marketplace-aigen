package vn.marketplace.payment.adapter.payment.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.EscrowStatus;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.PaymentStatus;
import vn.marketplace.payment.domain.payment.management.Payment;

/**
 * Pins the detached-entity merge semantics of {@code PaymentOa} on msfw's
 * {@code AbstractMementoJpaOa}: escrow-hold rows must be UPDATEd in place across
 * load → mutate → save cycles (the unique {@code hold_id} index would reject any
 * delete+re-insert or duplicate insert), and the root-attribute lookups must work
 * through the generic {@code Criteria} → Specification translator.
 */
@DataJpaTest
@Import(PaymentOa.class)
class PaymentOaJpaTest {

    @Autowired
    private PaymentOa paymentOa;

    @Autowired
    private PaymentJpaRepository jpa;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Payment freshPayment() {
        return Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1",
                Money.of(1_000_000, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", 600_000),
                        new Payment.NewAllocation("H-2", "O-2", "M-2", 400_000)));
    }

    @Test
    void saveLoadMutateSaveUpdatesHoldRowsInPlace() {
        Payment payment = freshPayment();
        paymentOa.save(payment);
        jpa.flush();
        assertTrue(payment._id() != null, "surrogate key threaded back on insert");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM escrow_holds", Integer.class));

        // load → confirm → save (webhook flow)
        Payment loaded = paymentOa.findById(new PaymentId("PAY-1")).orElseThrow();
        loaded.confirmPayment("TXN-1", 1_000_000);
        paymentOa.save(loaded);
        jpa.flush();

        // load via the holds join → release one hold → save (settlement flow)
        Payment byHold = paymentOa.findByHoldOrderId("O-1").orElseThrow();
        assertEquals(PaymentStatus.PAID, byHold.status());
        byHold.releaseForOrder("O-1");
        paymentOa.save(byHold);
        jpa.flush();

        // still exactly two hold rows — merged in place, never re-inserted
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM escrow_holds", Integer.class));
        Payment reloaded = paymentOa.findById(new PaymentId("PAY-1")).orElseThrow();
        assertEquals(EscrowStatus.RELEASED, reloaded.releaseForOrder("O-1").status(), "idempotent re-release");
        assertEquals(EscrowStatus.HELD, reloaded.holds().stream()
                .filter(h -> h.orderId().equals("O-2")).findFirst().orElseThrow().status());
        assertEquals("TXN-1", reloaded.gatewayTxnId());
    }

    @Test
    void rootAttributeLookupsGoThroughTheCriteriaTranslator() {
        paymentOa.save(freshPayment());
        jpa.flush();

        List<Payment> byOrderRef = paymentOa.findBy(Criteria.where("orderRef").eq("CHK-1"));
        assertEquals(1, byOrderRef.size());
        assertEquals("PAY-1", byOrderRef.get(0).id().value());

        assertEquals(0, paymentOa.findBy(Criteria.where("gatewayTxnId").eq("TXN-404")).size());
    }
}
