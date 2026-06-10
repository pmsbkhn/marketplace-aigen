package vn.marketplace.checkout.application.checkout;

import java.util.List;

/**
 * Outbound port to Inventory ({@code Inventory.ReserveStock/ReleaseStock}). Reserve is
 * all-or-nothing: {@code allReserved=false} means NOTHING is held. {@code orderRef} (= the
 * idempotency key) makes a saga retry safe — Inventory dedups by (orderRef, sku).
 */
public interface InventoryPort {

    ReservationDto reserveStock(String orderRef, List<ReserveItemDto> items);

    /** Idempotent on Inventory's side; releasing a CONSUMED/RELEASED reservation is a no-op. */
    void releaseStock(String reservationId);

    record ReserveItemDto(String sku, int quantity) {
    }

    record ReservationDto(String reservationId, boolean allReserved, List<ShortageDto> shortages) {
    }

    record ShortageDto(String sku, int available) {
    }
}
