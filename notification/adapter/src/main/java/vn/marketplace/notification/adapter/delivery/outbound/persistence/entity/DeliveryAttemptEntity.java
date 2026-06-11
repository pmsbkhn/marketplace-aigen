package vn.marketplace.notification.adapter.delivery.outbound.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "delivery_attempts")
@Getter
@Setter
public class DeliveryAttemptEntity extends LongIdJpaEntity {

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "provider")
    private String provider;

    @Column(name = "result", nullable = false, length = 32)
    private String result;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
}
