package vn.marketplace.inventory.adapter.stock.outbound.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.inventory.adapter.stock.outbound.persistence.entity.StockEntity;

/**
 * Spring Data contract behind {@code StockOa}. Identity and root-attribute lookups go through the
 * inherited Specification executor ({@code Criteria} DSL); the only hand-written query is the one
 * the translator cannot express — joining the reservations collection.
 */
public interface StockJpaRepository extends JpaOaRepository<StockEntity> {

    @Query("select distinct s from StockEntity s join s.reservations r where r.orderRef = :orderRef")
    List<StockEntity> findByOrderRef(@Param("orderRef") String orderRef);
}
