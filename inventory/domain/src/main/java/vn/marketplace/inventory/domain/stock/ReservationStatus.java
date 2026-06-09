package vn.marketplace.inventory.domain.stock;

/**
 * Reservation lifecycle — one-directional, terminal states immutable:
 * {@code HELD → RELEASED} (checkout fail / TTL expiry) or {@code HELD → CONSUMED} (order completed).
 * Never returns to HELD; release/consume on a terminal reservation is a no-op.
 */
public enum ReservationStatus {
    HELD,
    RELEASED,
    CONSUMED
}
