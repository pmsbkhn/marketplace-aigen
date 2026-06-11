package vn.marketplace.inventory.domain.stock.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.Snapshotable;
import vn.marketplace.inventory.domain.stock.MerchantId;
import vn.marketplace.inventory.domain.stock.ReservationId;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.shared.InventoryDomainException;
import vn.marketplace.inventory.domain.shared.InventoryErrorCode;

/**
 * Inventory aggregate root — the single authority over physical stock for one SKU.
 *
 * <p>Enforced invariants:
 * <ul>
 *   <li><b>Never oversell:</b> {@code available} can never go below 0; a reserve for more than
 *       {@code available} throws {@code INSUFFICIENT_STOCK}.</li>
 *   <li><b>Balance:</b> every reserve moves units {@code available → reserved}; release moves them
 *       back; consume removes them from {@code reserved} permanently.</li>
 *   <li><b>Reservation lifecycle:</b> {@code HELD → RELEASED | CONSUMED}, one-directional; release /
 *       consume of a terminal reservation is a no-op (idempotent).</li>
 *   <li><b>Reserve idempotency:</b> a second reserve for the same {@code orderRef} returns the
 *       existing hold rather than reserving twice.</li>
 *   <li><b>Tenant immutable / scoped:</b> {@code merchantId} is set at init and never changes; a
 *       merchant stock update by a different tenant throws {@code TENANT_MISMATCH}.</li>
 * </ul>
 * State changes only ever happen through the verb methods below — there are no public setters.
 */
public class Stock extends Aggregate<SkuCode> implements Snapshotable<Stock.Memento> {

    private final SkuCode sku;
    private final MerchantId merchantId;
    private int available;
    private int reserved;
    private final List<Reservation> reservations;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** New stock: available = reserved = 0. Package-private — built via {@link #initFor}. */
    private Stock(SkuCode sku, MerchantId merchantId) {
        this.id = sku;
        this.sku = sku;
        this.merchantId = merchantId;
        this.available = 0;
        this.reserved = 0;
        this.reservations = new ArrayList<>();
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Reconstruction constructor — restores full state, no transition. */
    private Stock(Memento m, List<Reservation> reservations) {
        this.id = new SkuCode(m.sku());
        this.set_id(m._id());
        this.sku = new SkuCode(m.sku());
        this.merchantId = new MerchantId(m.merchantId());
        this.available = m.available();
        this.reserved = m.reserved();
        this.reservations = new ArrayList<>(reservations);
        this.version = m.version();
        this.createdAt = m.createdAt();
        this.updatedAt = m.updatedAt();
    }

    /** Initialise a SKU at zero stock (consumed from Catalog's {@code ProductCreated}). */
    public static Stock initFor(SkuCode sku, MerchantId merchantId) {
        return new Stock(sku, merchantId);
    }

    // ---- verb methods (the only way to change state) ----

    /**
     * Reserve {@code qty} units for {@code orderRef}. Idempotent by {@code orderRef}: a second call
     * returns the existing HELD reservation without moving stock again.
     *
     * @throws InventoryDomainException INVALID_QUANTITY (qty ≤ 0) or INSUFFICIENT_STOCK (qty &gt; available)
     */
    public Reservation reserve(int qty, String orderRef, ReservationId reservationId, LocalDateTime expiresAt) {
        requirePositive(qty);
        Optional<Reservation> existing = heldReservationFor(orderRef);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (available < qty) {
            throw new InventoryDomainException(InventoryErrorCode.INSUFFICIENT_STOCK);
        }
        available -= qty;
        reserved += qty;
        Reservation reservation = new Reservation(reservationId, orderRef, sku, qty, expiresAt);
        reservations.add(reservation);
        touch();
        return reservation;
    }

    /** Release every HELD reservation for {@code orderRef}, returning units to {@code available}. No-op if none/terminal. */
    public void releaseReservation(String orderRef) {
        for (Reservation r : reservations) {
            if (r.orderRef().equals(orderRef) && r.isHeld()) {
                r.release();
                available += r.qty();
                reserved -= r.qty();
                touch();
            }
        }
    }

    /** Consume every HELD reservation for {@code orderRef} (permanent deduction from {@code reserved}). No-op if none/terminal. */
    public void consumeReservation(String orderRef) {
        for (Reservation r : reservations) {
            if (r.orderRef().equals(orderRef) && r.isHeld()) {
                r.consume();
                reserved -= r.qty();
                touch();
            }
        }
    }

    /** Merchant sets the absolute available quantity for their own SKU (tenant-scoped, qty ≥ 0). */
    public void changeAvailable(int qty, MerchantId caller) {
        if (caller == null || !merchantId.equals(caller)) {
            throw new InventoryDomainException(InventoryErrorCode.TENANT_MISMATCH);
        }
        if (qty < 0) {
            throw new InventoryDomainException(InventoryErrorCode.NEGATIVE_STOCK);
        }
        available = qty;
        touch();
    }

    private static void requirePositive(int qty) {
        if (qty <= 0) {
            throw new InventoryDomainException(InventoryErrorCode.INVALID_QUANTITY);
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    // ---- queries / accessors ----

    public Optional<Reservation> heldReservationFor(String orderRef) {
        return reservations.stream()
                .filter(r -> r.orderRef().equals(orderRef) && r.isHeld())
                .findFirst();
    }

    public SkuCode sku() {
        return sku;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public int available() {
        return available;
    }

    public int reserved() {
        return reserved;
    }

    public long version() {
        return version;
    }

    public List<Reservation> reservations() {
        return List.copyOf(reservations);
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    // ---- Memento (persistence reconstruction) ----

    public static Stock restore(Memento m) {
        List<Reservation> reservations = m.reservations().stream().map(Reservation::fromMemento).toList();
        return new Stock(m, reservations);
    }

    @Override
    public Memento snapshot() {
        List<ReservationMemento> reservationMementos = new ArrayList<>();
        for (Reservation r : reservations) {
            reservationMementos.add(new ReservationMemento(r.id().value(), r.orderRef(), r.sku().value(),
                    r.qty(), r.status().name(), r.expiresAt()));
        }
        return new Memento(_id(), sku.value(), merchantId.value(), available, reserved, version,
                createdAt, updatedAt, reservationMementos);
    }

    public record Memento(Long _id,
                          String sku,
                          String merchantId,
                          int available,
                          int reserved,
                          long version,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          List<ReservationMemento> reservations) {
    }

    public record ReservationMemento(String reservationId,
                                     String orderRef,
                                     String sku,
                                     int qty,
                                     String status,
                                     LocalDateTime expiresAt) {
    }
}
