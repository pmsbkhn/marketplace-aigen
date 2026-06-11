package vn.marketplace.inventory.adapter.stock.outbound.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_order_ref", columnList = "order_ref")
})
@Getter
@Setter
public class ReservationEntity extends LongIdJpaEntity {

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "order_ref", nullable = false)
    private String orderRef;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
