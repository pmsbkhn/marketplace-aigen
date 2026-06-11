package vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "skus")
@Getter
@Setter
public class SkuEntity extends LongIdJpaEntity {

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Column(name = "price_amount", nullable = false)
    private long priceAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
