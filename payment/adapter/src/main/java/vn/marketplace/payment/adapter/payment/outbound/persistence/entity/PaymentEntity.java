package vn.marketplace.payment.adapter.payment.outbound.persistence.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The {@code gateway_txn_id} UNIQUE index is the final dedup authority for webhook processing —
 * two transactions can never consume the same gateway txn (TC-PAY-INT-04).
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_payment_id", columnList = "payment_id", unique = true),
        @Index(name = "idx_payments_order_ref", columnList = "order_ref", unique = true),
        @Index(name = "idx_payments_gateway_txn", columnList = "gateway_txn_id", unique = true)
})
@Getter
@Setter
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @Column(name = "order_ref", nullable = false, unique = true)
    private String orderRef;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "gateway_txn_id", unique = true)
    private String gatewayTxnId;

    @Column(name = "payment_url", length = 1024)
    private String paymentUrl;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "payment_fk")
    @OrderColumn(name = "hold_order")
    private List<EscrowHoldEntity> holds = new ArrayList<>();
}
