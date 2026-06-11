package vn.marketplace.payment.adapter.payment.outbound.persistence;

import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.SettlementEntity;

/**
 * Spring Data contract behind {@code SettlementOa}. All lookups (settlementId, orderId, merchantId)
 * are root attributes, fully covered by the inherited Specification executor ({@code Criteria} DSL)
 * — no hand-written queries.
 */
public interface SettlementJpaRepository extends JpaOaRepository<SettlementEntity> {
}
