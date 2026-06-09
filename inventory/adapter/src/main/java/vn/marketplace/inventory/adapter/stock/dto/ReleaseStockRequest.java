package vn.marketplace.inventory.adapter.stock.dto;

/** Body for the internal release endpoint (stand-in for gRPC {@code Inventory.ReleaseStock}). */
public record ReleaseStockRequest(String reservationId) {
}
