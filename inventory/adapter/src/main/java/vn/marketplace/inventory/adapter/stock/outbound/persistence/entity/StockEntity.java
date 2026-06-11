package vn.marketplace.inventory.adapter.stock.outbound.persistence.entity;

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
@Table(name = "stock", indexes = {
        @Index(name = "idx_stock_sku", columnList = "sku", unique = true),
        @Index(name = "idx_stock_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class StockEntity extends LongIdJpaEntity {

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "available", nullable = false)
    private int available;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "stock_version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "stock_fk")
    @OrderColumn(name = "reservation_order")
    private List<ReservationEntity> reservations = new ArrayList<>();
}
