package vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.vsf.ptnt.springcore.persistence.LongIdJpaEntity;

@Entity
@Table(name = "variants")
@Getter
@Setter
public class VariantEntity extends LongIdJpaEntity {

    @Column(name = "variant_id", nullable = false)
    private String variantId;

    @Column(name = "name")
    private String name;

    @ElementCollection
    @CollectionTable(name = "variant_attributes", joinColumns = @JoinColumn(name = "variant_fk"))
    @MapKeyColumn(name = "attr_key")
    @Column(name = "attr_value")
    private Map<String, String> attributes = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "variant_fk")
    @OrderColumn(name = "sku_order")
    private List<SkuEntity> skus = new ArrayList<>();
}
