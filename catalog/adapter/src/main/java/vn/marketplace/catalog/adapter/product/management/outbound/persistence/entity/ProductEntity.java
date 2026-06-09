package vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity;

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

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_product_id", columnList = "product_id", unique = true),
        @Index(name = "idx_products_merchant", columnList = "merchant_id"),
        @Index(name = "idx_products_status", columnList = "status")
})
@Getter
@Setter
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "description", length = 10000)
    private String description;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "brand_id")
    private String brandId;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "product_created_published", nullable = false)
    private boolean productCreatedPublished;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "moderated_by")
    private String moderatedBy;

    @Column(name = "moderation_reason")
    private String moderationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_fk")
    @OrderColumn(name = "variant_order")
    private List<VariantEntity> variants = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_fk")
    @OrderColumn(name = "image_order")
    private List<ProductImageEntity> images = new ArrayList<>();
}
