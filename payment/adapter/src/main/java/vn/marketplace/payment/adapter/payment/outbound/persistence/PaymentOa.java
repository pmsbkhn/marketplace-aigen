package vn.marketplace.payment.adapter.payment.outbound.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.springcore.persistence.AbstractMementoJpaOa;
import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.EscrowHoldEntity;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.PaymentEntity;
import vn.marketplace.payment.application.payment.PaymentRepository;
import vn.marketplace.payment.domain.payment.management.Payment;

/**
 * Outbound persistence adapter: maps the {@link Payment} aggregate ↔ JPA entities at the
 * {@link Payment.Memento} level via msfw's {@code AbstractMementoJpaOa}, which owns the plumbing
 * (surrogate-key threading, identity lookups, {@code Criteria} → Specification translation, real
 * DB paging). {@code @Transactional} keeps the session open while the lazy holds collection is
 * read during reconstruction. The {@code gateway_txn_id} UNIQUE constraint makes the database the
 * final webhook-dedup authority (TC-PAY-INT-04).
 *
 * <p>Escrow holds carry their surrogate key through {@link Payment.HoldMemento#_id()}: on save the
 * detached child entities keep their ids, so the merge UPDATEs each hold row in place (clear+rebuild
 * would re-insert the same unique {@code hold_id} before the delete flushes → duplicate-key
 * violation). Holds are never removed; only their status flips HELD → RELEASED.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class PaymentOa extends AbstractMementoJpaOa<Payment, Payment.Memento, PaymentEntity>
        implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    protected JpaOaRepository<PaymentEntity> jpa() {
        return paymentJpaRepository;
    }

    /** Joins the holds collection — beyond the Criteria translator, so a derived query. */
    @Override
    public Optional<Payment> findByHoldOrderId(String orderId) {
        return paymentJpaRepository.findByHolds_OrderId(orderId).stream()
                .findFirst()
                .map(this::reconstitute);
    }

    @Override
    protected PaymentEntity fromMemento(Payment.Memento m) {
        PaymentEntity entity = new PaymentEntity();
        entity.setPaymentId(m.paymentId());
        entity.setOrderRef(m.orderRef());
        entity.setAmount(m.amount());
        entity.setCurrency(m.currency());
        entity.setStatus(m.status());
        entity.setGatewayTxnId(m.gatewayTxnId());
        entity.setPaymentUrl(m.paymentUrl());
        entity.setFailReason(m.failReason());
        entity.setCreatedAt(m.createdAt());
        entity.setPaidAt(m.paidAt());
        entity.setUpdatedAt(m.updatedAt());

        for (Payment.HoldMemento hm : m.holds()) {
            EscrowHoldEntity he = new EscrowHoldEntity();
            he.setId(hm._id()); // child surrogate key — keeps the merge an in-place UPDATE
            he.setVersion(hm._version()); // and the child's optimistic-lock version, or the merge reads as stale
            he.setHoldId(hm.holdId());
            he.setOrderId(hm.orderId());
            he.setMerchantId(hm.merchantId());
            he.setAmount(hm.amount());
            he.setCurrency(hm.currency());
            he.setStatus(hm.status());
            he.setReleasedAt(hm.releasedAt());
            entity.getHolds().add(he);
        }
        return entity;
    }

    @Override
    protected Payment.Memento toMemento(PaymentEntity e) {
        List<Payment.HoldMemento> holds = new ArrayList<>();
        for (EscrowHoldEntity he : e.getHolds()) {
            holds.add(new Payment.HoldMemento(he.getHoldId(), he.getOrderId(), he.getMerchantId(),
                    he.getAmount(), he.getCurrency(), he.getStatus(), he.getReleasedAt(), he.getId(),
                    he.getVersion()));
        }
        return new Payment.Memento(e.getId(), e.getPaymentId(), e.getOrderRef(), e.getAmount(),
                e.getCurrency(), e.getStatus(), e.getGatewayTxnId(), e.getPaymentUrl(),
                e.getFailReason(), e.getCreatedAt(), e.getPaidAt(), e.getUpdatedAt(), holds);
    }

    @Override
    protected Payment restore(Payment.Memento memento) {
        return Payment.restore(memento);
    }

    @Override
    protected Criteria identityCriteria(Identity<?> id) {
        return Criteria.where("paymentId").eq(id.value());
    }
}
