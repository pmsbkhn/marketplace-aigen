package vn.marketplace.catalog.adapter.product.management.outbound.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductEntity;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByProductId(String productId);

    List<ProductEntity> findByStatus(String status);

    void deleteByProductId(String productId);

    @Query("select distinct p from ProductEntity p join p.variants v join v.skus s where s.skuCode in :codes")
    List<ProductEntity> findBySkuCodes(@Param("codes") Collection<String> codes);
}
