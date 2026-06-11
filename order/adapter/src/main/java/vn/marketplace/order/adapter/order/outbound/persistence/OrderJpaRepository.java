package vn.marketplace.order.adapter.order.outbound.persistence;

import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderEntity;

/**
 * Spring Data contract for {@link OrderOa}. No hand-written finders: identity lookup goes through
 * {@code identityCriteria} ({@code orderId}) and all other filters ({@code checkoutRef},
 * {@code buyerId}, {@code merchantId}, {@code status}) are simple root attributes covered by the
 * msfw {@code Criteria} DSL → Specification translation.
 */
public interface OrderJpaRepository extends JpaOaRepository<OrderEntity> {
}
