package vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "product_images")
@Getter
@Setter
public class ProductImageEntity extends LongIdJpaEntity {

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
