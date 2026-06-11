package vn.marketplace.payment.adapter.payment.outbound.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;
import vn.marketplace.payment.adapter.payment.outbound.persistence.BankAccountCryptoConverter;

/**
 * Settlement + its embedded payout instruction. {@code bank_account_encrypted} is L4 data — written
 * through {@link BankAccountCryptoConverter}, so the column only ever holds ciphertext
 * (TC-PAY-INT-02). One settlement per order ({@code order_id} UNIQUE).
 */
@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlements_settlement_id", columnList = "settlement_id", unique = true),
        @Index(name = "idx_settlements_order", columnList = "order_id", unique = true),
        @Index(name = "idx_settlements_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class SettlementEntity extends LongIdJpaEntity {

    @Column(name = "settlement_id", nullable = false, unique = true)
    private String settlementId;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "gross_amount", nullable = false)
    private long grossAmount;

    @Column(name = "commission", nullable = false)
    private long commission;

    @Column(name = "net_amount", nullable = false)
    private long netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "doc_uri", length = 1024)
    private String docUri;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- payout instruction (entity inside the settlement aggregate) ----

    @Column(name = "payout_id", nullable = false, unique = true)
    private String payoutId;

    @Convert(converter = BankAccountCryptoConverter.class)
    @Column(name = "bank_account_encrypted", nullable = false, length = 512)
    private String bankAccount;

    @Column(name = "payout_amount", nullable = false)
    private long payoutAmount;

    @Column(name = "payout_status", nullable = false, length = 16)
    private String payoutStatus;

    @Column(name = "payout_submitted_at")
    private LocalDateTime payoutSubmittedAt;
}
