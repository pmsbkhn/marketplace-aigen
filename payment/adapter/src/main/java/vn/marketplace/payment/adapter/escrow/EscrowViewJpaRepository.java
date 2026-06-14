package vn.marketplace.payment.adapter.escrow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EscrowViewJpaRepository extends JpaRepository<EscrowViewEntity, String> {
}
