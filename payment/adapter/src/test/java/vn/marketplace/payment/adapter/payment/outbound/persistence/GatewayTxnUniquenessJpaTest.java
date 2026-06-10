package vn.marketplace.payment.adapter.payment.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.PaymentEntity;

/**
 * TC-PAY-INT-04 — the {@code gateway_txn_id} UNIQUE constraint is the last line of defence against
 * concurrent duplicate webhooks: whichever transaction commits second hits a
 * {@link DataIntegrityViolationException}, proving the database (not application code) is the
 * final dedup authority. (The spec stages two threads; the constraint that decides the race is
 * exercised here directly — the second insert with the same txn id is rejected.)
 */
@DataJpaTest
class GatewayTxnUniquenessJpaTest {

    @Autowired
    private PaymentJpaRepository payments;

    private PaymentEntity paid(String paymentId, String orderRef, String gatewayTxnId) {
        PaymentEntity e = new PaymentEntity();
        e.setPaymentId(paymentId);
        e.setOrderRef(orderRef);
        e.setAmount(1_000_000);
        e.setCurrency("VND");
        e.setStatus("PAID");
        e.setGatewayTxnId(gatewayTxnId);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void secondPaymentWithSameGatewayTxnIdViolatesUniqueConstraint() {
        payments.saveAndFlush(paid("PAY-1", "CHK-1", "TXN-DUP"));

        assertThrows(DataIntegrityViolationException.class,
                () -> payments.saveAndFlush(paid("PAY-2", "CHK-2", "TXN-DUP")));
    }

    @Test
    void distinctTxnIdsCoexist() {
        payments.saveAndFlush(paid("PAY-1", "CHK-1", "TXN-1"));
        payments.saveAndFlush(paid("PAY-2", "CHK-2", "TXN-2"));
    }
}
