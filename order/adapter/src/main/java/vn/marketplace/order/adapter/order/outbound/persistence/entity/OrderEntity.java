package vn.marketplace.order.adapter.order.outbound.persistence.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_orders_checkout_ref", columnList = "checkout_ref", unique = true),
        @Index(name = "idx_orders_buyer", columnList = "buyer_id"),
        @Index(name = "idx_orders_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class OrderEntity extends LongIdJpaEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "checkout_ref", nullable = false, unique = true)
    private String checkoutRef;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // Shipping address (PII) — field-level encrypted at rest in production; plain columns here.
    @Column(name = "addr_full_name")
    private String addrFullName;
    @Column(name = "addr_phone")
    private String addrPhone;
    @Column(name = "addr_line")
    private String addrLine;
    @Column(name = "addr_ward")
    private String addrWard;
    @Column(name = "addr_district")
    private String addrDistrict;
    @Column(name = "addr_city")
    private String addrCity;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_fk")
    @OrderColumn(name = "item_order")
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_fk")
    @OrderColumn(name = "history_order")
    private List<OrderStatusHistoryEntity> statusHistory = new ArrayList<>();
}
