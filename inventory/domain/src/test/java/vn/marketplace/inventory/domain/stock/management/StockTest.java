package vn.marketplace.inventory.domain.stock.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import vn.marketplace.inventory.domain.shared.InventoryDomainException;
import vn.marketplace.inventory.domain.shared.InventoryErrorCode;
import vn.marketplace.inventory.domain.stock.MerchantId;
import vn.marketplace.inventory.domain.stock.ReservationId;
import vn.marketplace.inventory.domain.stock.ReservationStatus;
import vn.marketplace.inventory.domain.stock.SkuCode;

class StockTest {

    private Stock stockWith(int available, String merchant) {
        Stock stock = Stock.initFor(new SkuCode("SKU-1"), new MerchantId(merchant));
        stock.changeAvailable(available, new MerchantId(merchant));
        return stock;
    }

    @Test
    void reserveMoreThanAvailableThrowsInsufficientStock() { // TC-INV-01
        Stock stock = stockWith(10, "M-111");

        InventoryDomainException ex = assertThrows(InventoryDomainException.class,
                () -> stock.reserve(15, "ORDER-1", new ReservationId("R-1"), null));

        assertEquals(InventoryErrorCode.INSUFFICIENT_STOCK, ex.inventoryErrorCode());
        assertEquals(10, stock.available());
        assertEquals(0, stock.reserved());
    }

    @Test
    void reserveMovesUnitsFromAvailableToReserved() { // TC-INV-02
        Stock stock = stockWith(10, "M-111");

        stock.reserve(5, "ORDER-1", new ReservationId("R-1"), null);

        assertEquals(5, stock.available());
        assertEquals(5, stock.reserved());
    }

    @Test
    void releaseOnConsumedReservationIsNoOp() { // TC-INV-03
        Stock stock = stockWith(10, "M-111");
        stock.reserve(5, "ORDER-1", new ReservationId("R-1"), null);
        stock.consumeReservation("ORDER-1");
        // after consume: available=5, reserved=0, reservation CONSUMED
        assertEquals(ReservationStatus.CONSUMED, stock.reservations().get(0).status());

        stock.releaseReservation("ORDER-1"); // must NOT return already-sold stock

        assertEquals(5, stock.available());
        assertEquals(0, stock.reserved());
        assertEquals(ReservationStatus.CONSUMED, stock.reservations().get(0).status());
    }

    @Test
    void crossTenantStockUpdateThrowsTenantMismatch() { // TC-INV-04
        Stock stock = stockWith(10, "M-111");

        InventoryDomainException ex = assertThrows(InventoryDomainException.class,
                () -> stock.changeAvailable(50, new MerchantId("M-999")));

        assertEquals(InventoryErrorCode.TENANT_MISMATCH, ex.inventoryErrorCode());
        assertEquals(10, stock.available());
    }

    @Test
    void reserveIsIdempotentByOrderRef() {
        Stock stock = stockWith(10, "M-111");

        stock.reserve(5, "ORDER-1", new ReservationId("R-1"), null);
        stock.reserve(5, "ORDER-1", new ReservationId("R-2"), null); // same orderRef → no double reserve

        assertEquals(5, stock.available());
        assertEquals(5, stock.reserved());
        assertEquals(1, stock.reservations().size());
    }

    @Test
    void releaseReturnsUnitsToAvailable() {
        Stock stock = stockWith(10, "M-111");
        stock.reserve(4, "ORDER-1", new ReservationId("R-1"), null);

        stock.releaseReservation("ORDER-1");

        assertEquals(10, stock.available());
        assertEquals(0, stock.reserved());
        assertEquals(ReservationStatus.RELEASED, stock.reservations().get(0).status());
    }
}
