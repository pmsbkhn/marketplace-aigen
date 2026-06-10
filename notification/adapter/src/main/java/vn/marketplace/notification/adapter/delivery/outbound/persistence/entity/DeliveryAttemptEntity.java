package vn.marketplace.notification.adapter.delivery.outbound.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "delivery_attempts")
@Getter
@Setter
public class DeliveryAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "provider")
    private String provider;

    @Column(name = "result", nullable = false, length = 32)
    private String result;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
}
