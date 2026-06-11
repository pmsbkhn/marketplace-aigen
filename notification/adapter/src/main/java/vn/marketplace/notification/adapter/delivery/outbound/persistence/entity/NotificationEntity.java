package vn.marketplace.notification.adapter.delivery.outbound.persistence.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_notification_id", columnList = "notification_id", unique = true),
        @Index(name = "idx_notifications_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_notifications_user", columnList = "user_id")
})
@Getter
@Setter
public class NotificationEntity extends LongIdJpaEntity {

    @Column(name = "notification_id", nullable = false, unique = true)
    private String notificationId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "channel", nullable = false, length = 16)
    private String channel;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    // Encrypted template variables (L4 — ciphertext values written by the application layer).
    @ElementCollection
    @CollectionTable(name = "notification_variables_encrypted", joinColumns = @JoinColumn(name = "notification_fk"))
    @MapKeyColumn(name = "var_key")
    @Column(name = "var_value_encrypted", length = 2000)
    private Map<String, String> variables = new HashMap<>();

    @Column(name = "priority", nullable = false, length = 16)
    private String priority;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "source_event_type")
    private String sourceEventType;

    @Column(name = "source_event_id")
    private String sourceEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "notification_fk")
    @OrderColumn(name = "attempt_order")
    private List<DeliveryAttemptEntity> attempts = new ArrayList<>();
}
