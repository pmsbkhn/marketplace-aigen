package vn.marketplace.inventory.adapter.stock.outbound.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.marketplace.inventory.adapter.stock.outbound.persistence.entity.StockEntity;

public interface StockJpaRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findBySku(String sku);

    List<StockEntity> findBySkuIn(Collection<String> skus);

    void deleteBySku(String sku);

    @Query("select distinct s from StockEntity s join s.reservations r where r.orderRef = :orderRef")
    List<StockEntity> findByOrderRef(@Param("orderRef") String orderRef);
}
