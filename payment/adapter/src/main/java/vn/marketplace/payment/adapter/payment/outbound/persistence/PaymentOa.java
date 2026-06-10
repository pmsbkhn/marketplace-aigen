package vn.marketplace.payment.adapter.payment.outbound.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.EscrowHoldEntity;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.PaymentEntity;
import vn.marketplace.payment.application.payment.PaymentCriteria;
import vn.marketplace.payment.domain.payment.management.Payment;

/**
 * Outbound persistence adapter: maps the {@link Payment} aggregate ↔ JPA entities via the aggregate
 * {@link Payment.Memento}, implementing the msfw {@code Repository<Payment>} port. The
 * {@code gateway_txn_id} UNIQUE constraint makes the database the final webhook-dedup authority
 * (TC-PAY-INT-04).
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class PaymentOa implements tech.vsf.ptnt.msfw.domain.core.Repository<Payment> {

    private final PaymentJpaRepository jpa;

    @Override
    public void save(Payment aggregate) {
        Payment.Memento m = aggregate.toMemento();
        PaymentEntity entity = jpa.findByPaymentId(m.paymentId()).orElseGet(PaymentEntity::new);

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

        // Escrow holds: update IN PLACE keyed by holdId (clear+rebuild would re-insert the same
        // unique hold_id before the delete flushes → duplicate-key violation). Holds are never
        // removed; only their status flips HELD → RELEASED.
        for (Payment.HoldMemento hm : m.holds()) {
            EscrowHoldEntity he = entity.getHolds().stream()
                    .filter(x -> hm.holdId().equals(x.getHoldId()))
                    .findFirst()
                    .orElseGet(() -> {
                        EscrowHoldEntity created = new EscrowHoldEntity();
                        entity.getHolds().add(created);
                        return created;
                    });
            he.setHoldId(hm.holdId());
            he.setOrderId(hm.orderId());
            he.setMerchantId(hm.merchantId());
            he.setAmount(hm.amount());
            he.setCurrency(hm.currency());
            he.setStatus(hm.status());
            he.setReleasedAt(hm.releasedAt());
        }

        PaymentEntity saved = jpa.saveAndFlush(entity);
        aggregate.set_id(saved.getId());
    }

    @Override
    public <U extends Identity<?>> Optional<Payment> findById(U id) {
        return jpa.findByPaymentId(String.valueOf(id.value())).map(this::toDomain);
    }

    @Override
    public List<Payment> findBy(Criteria criteria) {
        PaymentCriteria c = (PaymentCriteria) criteria;
        List<PaymentEntity> entities;
        if (c.orderRef() != null) {
            entities = jpa.findByOrderRef(c.orderRef()).map(List::of).orElseGet(List::of);
        } else if (c.gatewayTxnId() != null) {
            entities = jpa.findByGatewayTxnId(c.gatewayTxnId()).map(List::of).orElseGet(List::of);
        } else if (c.orderId() != null) {
            entities = jpa.findByHolds_OrderId(c.orderId());
        } else {
            entities = jpa.findAll();
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public PagedSearchResult<Payment> findBy(Criteria criteria, Pagination pagination) {
        List<Payment> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Payment> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        jpa.deleteByPaymentId(String.valueOf(id.value()));
    }

    private Payment toDomain(PaymentEntity e) {
        List<Payment.HoldMemento> holds = new ArrayList<>();
        for (EscrowHoldEntity he : e.getHolds()) {
            holds.add(new Payment.HoldMemento(he.getHoldId(), he.getOrderId(), he.getMerchantId(),
                    he.getAmount(), he.getCurrency(), he.getStatus(), he.getReleasedAt()));
        }
        Payment.Memento m = new Payment.Memento(e.getId(), e.getPaymentId(), e.getOrderRef(),
                e.getAmount(), e.getCurrency(), e.getStatus(), e.getGatewayTxnId(), e.getPaymentUrl(),
                e.getFailReason(), e.getCreatedAt(), e.getPaidAt(), e.getUpdatedAt(), holds);
        return Payment.fromMemento(m);
    }
}
