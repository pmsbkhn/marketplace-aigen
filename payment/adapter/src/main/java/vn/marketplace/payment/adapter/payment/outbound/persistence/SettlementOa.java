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
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.SettlementEntity;
import vn.marketplace.payment.application.payment.SettlementCriteria;
import vn.marketplace.payment.domain.payment.management.Settlement;

/**
 * Outbound persistence adapter for the {@link Settlement} aggregate. The merchant bank account
 * passes through {@code BankAccountCryptoConverter} — the column only ever stores ciphertext
 * (TC-PAY-INT-02).
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class SettlementOa implements tech.vsf.ptnt.msfw.domain.core.Repository<Settlement> {

    private final SettlementJpaRepository jpa;

    @Override
    public void save(Settlement aggregate) {
        Settlement.Memento m = aggregate.toMemento();
        SettlementEntity entity = jpa.findBySettlementId(m.settlementId()).orElseGet(SettlementEntity::new);

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

        SettlementEntity saved = jpa.saveAndFlush(entity);
        aggregate.set_id(saved.getId());
    }

    @Override
    public <U extends Identity<?>> Optional<Settlement> findById(U id) {
        return jpa.findBySettlementId(String.valueOf(id.value())).map(this::toDomain);
    }

    @Override
    public List<Settlement> findBy(Criteria criteria) {
        SettlementCriteria c = (SettlementCriteria) criteria;
        List<SettlementEntity> entities;
        if (c.orderId() != null) {
            entities = jpa.findByOrderId(c.orderId()).map(List::of).orElseGet(List::of);
        } else if (c.merchantId() != null) {
            entities = jpa.findByMerchantId(c.merchantId());
        } else {
            entities = jpa.findAll();
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public PagedSearchResult<Settlement> findBy(Criteria criteria, Pagination pagination) {
        List<Settlement> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Settlement> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        jpa.deleteBySettlementId(String.valueOf(id.value()));
    }

    private Settlement toDomain(SettlementEntity e) {
        Settlement.PayoutMemento payout = new Settlement.PayoutMemento(e.getPayoutId(), e.getMerchantId(),
                e.getBankAccount(), e.getPayoutAmount(), e.getCurrency(), e.getPayoutStatus(),
                e.getPayoutSubmittedAt());
        Settlement.Memento m = new Settlement.Memento(e.getId(), e.getSettlementId(), e.getOrderId(),
                e.getMerchantId(), e.getGrossAmount(), e.getCommission(), e.getNetAmount(),
                e.getCurrency(), e.getStatus(), e.getDocUri(), e.getCreatedAt(), e.getCompletedAt(),
                e.getUpdatedAt(), payout);
        return Settlement.fromMemento(m);
    }
}
