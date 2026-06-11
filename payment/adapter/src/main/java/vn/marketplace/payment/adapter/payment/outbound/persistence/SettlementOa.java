package vn.marketplace.payment.adapter.payment.outbound.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.springcore.persistence.AbstractMementoJpaOa;
import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.SettlementEntity;
import vn.marketplace.payment.domain.payment.management.Settlement;

/**
 * Outbound persistence adapter for the {@link Settlement} aggregate, mapping at the
 * {@link Settlement.Memento} level via msfw's {@code AbstractMementoJpaOa} (surrogate-key
 * threading, identity lookups, {@code Criteria} → Specification translation, real DB paging all
 * inherited). The merchant bank account passes through {@code BankAccountCryptoConverter} — the
 * column only ever stores ciphertext (TC-PAY-INT-02).
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class SettlementOa extends AbstractMementoJpaOa<Settlement, Settlement.Memento, SettlementEntity> {

    private final SettlementJpaRepository settlementJpaRepository;

    @Override
    protected JpaOaRepository<SettlementEntity> jpa() {
        return settlementJpaRepository;
    }

    @Override
    protected SettlementEntity fromMemento(Settlement.Memento m) {
        SettlementEntity entity = new SettlementEntity();
        entity.setSettlementId(m.settlementId());
        entity.setOrderId(m.orderId());
        entity.setMerchantId(m.merchantId());
        entity.setGrossAmount(m.grossAmount());
        entity.setCommission(m.commission());
        entity.setNetAmount(m.netAmount());
        entity.setCurrency(m.currency());
        entity.setStatus(m.status());
        entity.setDocUri(m.docUri());
        entity.setCreatedAt(m.createdAt());
        entity.setCompletedAt(m.completedAt());
        entity.setUpdatedAt(m.updatedAt());

        Settlement.PayoutMemento pm = m.payout();
        entity.setPayoutId(pm.payoutId());
        entity.setBankAccount(pm.bankAccount());
        entity.setPayoutAmount(pm.amount());
        entity.setPayoutStatus(pm.status());
        entity.setPayoutSubmittedAt(pm.submittedAt());
        return entity;
    }

    @Override
    protected Settlement.Memento toMemento(SettlementEntity e) {
        Settlement.PayoutMemento payout = new Settlement.PayoutMemento(e.getPayoutId(),
                e.getMerchantId(), e.getBankAccount(), e.getPayoutAmount(), e.getCurrency(),
                e.getPayoutStatus(), e.getPayoutSubmittedAt());
        return new Settlement.Memento(e.getId(), e.getSettlementId(), e.getOrderId(),
                e.getMerchantId(), e.getGrossAmount(), e.getCommission(), e.getNetAmount(),
                e.getCurrency(), e.getStatus(), e.getDocUri(), e.getCreatedAt(), e.getCompletedAt(),
                e.getUpdatedAt(), payout);
    }

    @Override
    protected Settlement restore(Settlement.Memento memento) {
        return Settlement.restore(memento);
    }

    @Override
    protected Criteria identityCriteria(Identity<?> id) {
        return Criteria.where("settlementId").eq(id.value());
    }
}
