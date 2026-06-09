package vn.marketplace.inventory.application.stock;

import java.util.List;

/**
 * Reserve command. {@code orderRef} is both the idempotency key and the handle later used to release
 * or consume the hold ({@link ReserveStockResult#reservationId()} echoes it back).
 */
public record ReserveStockCmd(String orderRef, List<StockItemInput> items) {

    /** One requested SKU + quantity. */
    public record StockItemInput(String sku, int quantity) {
    }
}
