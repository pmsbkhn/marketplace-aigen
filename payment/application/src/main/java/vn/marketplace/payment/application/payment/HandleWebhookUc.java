package vn.marketplace.payment.application.payment;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.payment.domain.payment.PaymentStatus;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * Drives the payment state machine from a verified gateway webhook. Idempotent: re-delivery of the
 * same {@code gatewayTxnId} for an already-PAID payment is a silent no-op; a different payment
 * claiming an already-used txn is rejected ({@code DUPLICATE_GATEWAY_TXN}) — and the DB UNIQUE
 * constraint on {@code gateway_txn_id} remains the last line of defence (TC-PAY-INT-04). The
 * amount cross-check (TC-PAY-01) lives in {@link Payment#confirmPayment}. State-writing →
 * {@link EventPublishHandler}: {@code PaymentReceived}/{@code PaymentFailed} drain to the outbox.
 */
public class HandleWebhookUc implements HandleWebhook {

    private final Repository<Payment> paymentRepository;

    public HandleWebhookUc(Repository<Payment> paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(HandleWebhookCmd cmd) {
        List<Payment> byRef = paymentRepository.findBy(Criteria.where("orderRef").eq(cmd.orderRef()));
        if (byRef.isEmpty()) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "No payment for orderRef " + cmd.orderRef());
        }
        Payment payment = byRef.get(0);

        // Idempotent re-delivery: this exact txn already confirmed this payment.
        if (payment.status() == PaymentStatus.PAID && cmd.gatewayTxnId().equals(payment.gatewayTxnId())) {
            return;
        }

        // Same txn id claimed by a different payment — replay/abuse; DB UNIQUE is the final guard.
        List<Payment> byTxn = paymentRepository.findBy(Criteria.where("gatewayTxnId").eq(cmd.gatewayTxnId()));
        if (!byTxn.isEmpty() && !byTxn.get(0).id().equals(payment.id())) {
            throw new PaymentDomainException(PaymentErrorCode.DUPLICATE_GATEWAY_TXN,
                    "Gateway txn " + cmd.gatewayTxnId() + " already consumed by another payment");
        }

        if (cmd.isSuccess()) {
            payment.confirmPayment(cmd.gatewayTxnId(), cmd.amount());
        } else {
            payment.markFailed("Gateway status: " + cmd.status());
        }

        paymentRepository.save(payment);
    }
}
