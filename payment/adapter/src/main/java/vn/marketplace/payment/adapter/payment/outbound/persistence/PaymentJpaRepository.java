package vn.marketplace.payment.adapter.payment.outbound.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.PaymentEntity;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByPaymentId(String paymentId);

    Optional<PaymentEntity> findByOrderRef(String orderRef);

    Optional<PaymentEntity> findByGatewayTxnId(String gatewayTxnId);

    List<PaymentEntity> findByHolds_OrderId(String orderId);

    void deleteByPaymentId(String paymentId);
}
