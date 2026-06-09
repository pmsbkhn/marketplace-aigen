package vn.marketplace.inventory.application.stock;

/** Release command. {@code reservationId} is the {@code orderRef} handle returned by reserve. */
public record ReleaseStockCmd(String reservationId) {
}
