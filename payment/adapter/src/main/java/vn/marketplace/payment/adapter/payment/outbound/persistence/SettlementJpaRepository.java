package vn.marketplace.payment.adapter.payment.outbound.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.SettlementEntity;

public interface SettlementJpaRepository extends JpaRepository<SettlementEntity, Long> {

    Optional<SettlementEntity> findBySettlementId(String settlementId);

    Optional<SettlementEntity> findByOrderId(String orderId);

    List<SettlementEntity> findByMerchantId(String merchantId);

    void deleteBySettlementId(String settlementId);
}
