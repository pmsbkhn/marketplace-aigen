package vn.marketplace.inventory.application.stock;

import java.util.List;

/** Query stock levels for these SKUs. */
public record GetStockLevelCmd(List<String> skuCodes) {
}
