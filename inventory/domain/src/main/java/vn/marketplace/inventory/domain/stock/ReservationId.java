package vn.marketplace.inventory.domain.stock;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Identity of a single {@link vn.marketplace.inventory.domain.stock.management.Reservation} (ULID/UUID). */
public class ReservationId extends StringIdentity {

    public ReservationId(String value) {
        super(value);
    }
}
