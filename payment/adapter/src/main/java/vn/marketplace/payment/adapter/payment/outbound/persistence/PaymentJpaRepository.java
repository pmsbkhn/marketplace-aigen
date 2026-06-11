package vn.marketplace.payment.adapter.payment.outbound.persistence;

import java.util.List;

import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.PaymentEntity;

/**
 * Spring Data contract behind {@code PaymentOa}. Identity and root-attribute lookups (paymentId,
 * orderRef, gatewayTxnId) go through the inherited Specification executor ({@code Criteria} DSL);
 * the only hand-written query is the one the translator cannot express — joining the escrow
 * {@code holds} collection.
 */
public interface PaymentJpaRepository extends JpaOaRepository<PaymentEntity> {

    /** The payment whose escrow holds contain an allocation for this order (collection join). */
    List<PaymentEntity> findByHolds_OrderId(String orderId);
}
