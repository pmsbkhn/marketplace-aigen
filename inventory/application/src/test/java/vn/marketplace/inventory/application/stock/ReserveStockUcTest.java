package vn.marketplace.inventory.application.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.marketplace.inventory.application.stock.ReserveStockCmd.StockItemInput;
import vn.marketplace.inventory.domain.stock.MerchantId;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.stock.management.Stock;

class ReserveStockUcTest {

    private InMemoryStockRepository repository;
    private InitSkuUc initUc;
    private ReserveStockUc reserveUc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository();
        initUc = new InitSkuUc(repository);
        reserveUc = new ReserveStockUc(repository);
    }

    private void givenStock(String sku, int available, String merchant) {
        Stock stock = Stock.initFor(new SkuCode(sku), new MerchantId(merchant));
        stock.changeAvailable(available, new MerchantId(merchant));
        repository.save(stock);
    }

    @Test
    void reservesWhenEnoughStock() {
        givenStock("SKU-1", 10, "M-1");
        givenStock("SKU-2", 5, "M-1");

        ReserveStockResult result = reserveUc.execute(new ReserveStockCmd("ORDER-1",
                List.of(new StockItemInput("SKU-1", 3), new StockItemInput("SKU-2", 5))));

        assertTrue(result.allReserved());
        assertEquals("ORDER-1", result.reservationId());
        assertEquals(7, repository.store.get("SKU-1").available());
        assertEquals(3, repository.store.get("SKU-1").reserved());
        assertEquals(0, repository.store.get("SKU-2").available());
    }

    @Test
    void reservesNothingWhenAnyItemShort() { // all-or-nothing
        givenStock("SKU-1", 10, "M-1");
        givenStock("SKU-2", 2, "M-1");

        ReserveStockResult result = reserveUc.execute(new ReserveStockCmd("ORDER-1",
                List.of(new StockItemInput("SKU-1", 3), new StockItemInput("SKU-2", 5))));

        assertFalse(result.allReserved());
        // nothing moved
        assertEquals(10, repository.store.get("SKU-1").available());
        assertEquals(0, repository.store.get("SKU-1").reserved());
        assertEquals(2, repository.store.get("SKU-2").available());
        String sku2Status = result.details().stream()
                .filter(d -> d.sku().equals("SKU-2")).findFirst().orElseThrow().status();
        assertEquals(ReserveStockResult.ItemReserveDetail.INSUFFICIENT_STOCK, sku2Status);
    }

    @Test
    void reserveIsIdempotentByOrderRef() {
        givenStock("SKU-1", 10, "M-1");
        ReserveStockCmd cmd = new ReserveStockCmd("ORDER-1", List.of(new StockItemInput("SKU-1", 4)));

        reserveUc.execute(cmd);
        ReserveStockResult second = reserveUc.execute(cmd); // replay

        assertTrue(second.allReserved());
        assertEquals(6, repository.store.get("SKU-1").available());
        assertEquals(4, repository.store.get("SKU-1").reserved());
        assertEquals(1, repository.store.get("SKU-1").reservations().size());
    }

    @Test
    void initSkuCreatesZeroStockAndIsIdempotent() {
        initUc.execute(new InitSkuCmd("evt-1", "P-1", List.of("SKU-9"), "M-1"));
        assertEquals(0, repository.store.get("SKU-9").available());

        // raise available, then replay init → must NOT reset
        repository.store.get("SKU-9").changeAvailable(50, new MerchantId("M-1"));
        initUc.execute(new InitSkuCmd("evt-1", "P-1", List.of("SKU-9"), "M-1"));
        assertEquals(50, repository.store.get("SKU-9").available());
    }
}
