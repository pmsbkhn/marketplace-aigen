package vn.marketplace.payment.adapter.payment.outbound.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "escrow_holds", indexes = {
        @Index(name = "idx_escrow_holds_order", columnList = "order_id")
})
@Getter
@Setter
public class EscrowHoldEntity extends LongIdJpaEntity {

    @Column(name = "hold_id", nullable = false, unique = true)
    private String holdId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
