package vn.marketplace.inventory.domain.stock.management;

import java.time.LocalDateTime;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.inventory.domain.stock.ReservationId;
import vn.marketplace.inventory.domain.stock.ReservationStatus;
import vn.marketplace.inventory.domain.stock.SkuCode;

/**
 * A stock hold for one SKU under one order, inside the {@link Stock} aggregate boundary — only ever
 * created/mutated through the aggregate, never independently.
 *
 * <p>Lifecycle {@code HELD → RELEASED | CONSUMED} is one-directional; {@link #release()} and
 * {@link #consume()} are <strong>no-ops on a terminal reservation</strong> (idempotent — never returns
 * already-sold or already-freed stock).
 */
public class Reservation implements Entity {

    private final ReservationId id;
    private final String orderRef;
    private final SkuCode sku;
    private final int qty;
    private final LocalDateTime expiresAt;
    private ReservationStatus status;

    Reservation(ReservationId id, String orderRef, SkuCode sku, int qty, LocalDateTime expiresAt) {
        this.id = id;
        this.orderRef = orderRef;
        this.sku = sku;
        this.qty = qty;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.HELD;
    }

    /** Reconstruction from persistence — restores status, no transition. */
    static Reservation fromMemento(Stock.ReservationMemento m) {
        Reservation r = new Reservation(new ReservationId(m.reservationId()), m.orderRef(),
                new SkuCode(m.sku()), m.qty(), m.expiresAt());
        r.status = ReservationStatus.valueOf(m.status());
        return r;
    }

    /** HELD → RELEASED. No-op if already terminal (idempotent). */
    void release() {
        if (status == ReservationStatus.HELD) {
            status = ReservationStatus.RELEASED;
        }
    }

    /** HELD → CONSUMED. No-op if already terminal (idempotent). */
    void consume() {
        if (status == ReservationStatus.HELD) {
            status = ReservationStatus.CONSUMED;
        }
    }

    public boolean isHeld() {
        return status == ReservationStatus.HELD;
    }

    public ReservationId id() {
        return id;
    }

    public String orderRef() {
        return orderRef;
    }

    public SkuCode sku() {
        return sku;
    }

    public int qty() {
        return qty;
    }

    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    public ReservationStatus status() {
        return status;
    }
}
