package vn.marketplace.inventory.application.stock;

import java.util.List;

/** Use-case port: read current stock levels for a set of SKUs (storefront display). Read-only. */
public interface GetStockLevel {
    List<StockLevelView> execute(GetStockLevelCmd cmd);
}
