package vn.marketplace.order.adapter.order.outbound.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderEntity;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    Optional<OrderEntity> findByCheckoutRef(String checkoutRef);

    List<OrderEntity> findByBuyerId(String buyerId);

    List<OrderEntity> findByMerchantId(String merchantId);

    void deleteByOrderId(String orderId);
}
