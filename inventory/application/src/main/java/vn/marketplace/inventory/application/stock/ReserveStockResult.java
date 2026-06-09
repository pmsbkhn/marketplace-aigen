package vn.marketplace.inventory.application.stock;

import java.util.List;

/**
 * Reserve outcome. {@code allReserved} is false when any SKU lacked stock — in that case nothing is
 * held (all-or-nothing) and {@code details} explains which SKUs were short. {@code reservationId} is
 * the {@code orderRef} handle for a later release/consume.
 */
public record ReserveStockResult(String reservationId, boolean allReserved, List<ItemReserveDetail> details) {

    /** Per-SKU result; {@code status} is {@code "OK"} or {@code "INSUFFICIENT_STOCK"}. */
    public record ItemReserveDetail(String sku, String status, int available) {
        public static final String OK = "OK";
        public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    }
}
