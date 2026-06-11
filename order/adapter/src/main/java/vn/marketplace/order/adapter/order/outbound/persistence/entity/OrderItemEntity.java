package vn.marketplace.order.adapter.order.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItemEntity extends LongIdJpaEntity {

    @Column(name = "order_item_id", nullable = false)
    private String orderItemId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "variant_id")
    private String variantId;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "product_name")
    private String productName;

    // Price snapshot — captured at creation, never updated (immutability invariant).
    @Column(name = "price_snapshot", nullable = false, updatable = false)
    private long priceSnapshot;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "qty", nullable = false)
    private int qty;
}
