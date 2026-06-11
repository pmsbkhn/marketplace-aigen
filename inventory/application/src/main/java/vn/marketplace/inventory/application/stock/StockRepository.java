package vn.marketplace.inventory.application.stock;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Persistence port for {@link Stock}. Extends the generic msfw {@code Repository<Stock>}
 * (identity lookups + {@code Criteria} DSL queries on root attributes) with the one lookup the
 * Criteria translator cannot express: finding stocks through their reservations collection.
 */
public interface StockRepository extends Repository<Stock> {

    /** Stocks holding a reservation (any status) for this order — used by release/consume flows. */
    List<Stock> findByOrderRef(String orderRef);
}
