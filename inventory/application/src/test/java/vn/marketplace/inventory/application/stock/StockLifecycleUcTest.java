package vn.marketplace.inventory.application.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.marketplace.inventory.application.stock.ReserveStockCmd.StockItemInput;
import vn.marketplace.inventory.domain.stock.MerchantId;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.stock.management.Stock;

/** Reserve → release / deduct lifecycle through the use-cases. */
class StockLifecycleUcTest {

    private InMemoryStockRepository repository;
    private ReserveStockUc reserveUc;
    private ReleaseStockUc releaseUc;
    private DeductStockUc deductUc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository();
        reserveUc = new ReserveStockUc(repository);
        releaseUc = new ReleaseStockUc(repository);
        deductUc = new DeductStockUc(repository);
        Stock stock = Stock.initFor(new SkuCode("SKU-1"), new MerchantId("M-1"));
        stock.changeAvailable(10, new MerchantId("M-1"));
        repository.save(stock);
    }

    @Test
    void releaseReturnsReservedUnits() {
        reserveUc.execute(new ReserveStockCmd("ORDER-1", List.of(new StockItemInput("SKU-1", 4))));

        releaseUc.execute(new ReleaseStockCmd("ORDER-1"));

        assertEquals(10, repository.store.get("SKU-1").available());
        assertEquals(0, repository.store.get("SKU-1").reserved());
    }

    @Test
    void deductConsumesReservedUnitsPermanently() {
        reserveUc.execute(new ReserveStockCmd("ORDER-1", List.of(new StockItemInput("SKU-1", 4))));

        deductUc.execute(new DeductStockCmd("evt-1", "ORDER-1", List.of("SKU-1")));

        assertEquals(6, repository.store.get("SKU-1").available()); // available unchanged by deduct
        assertEquals(0, repository.store.get("SKU-1").reserved());  // reserved removed
    }

    @Test
    void deductIsIdempotentAndReleaseAfterDeductIsNoOp() {
        reserveUc.execute(new ReserveStockCmd("ORDER-1", List.of(new StockItemInput("SKU-1", 4))));
        deductUc.execute(new DeductStockCmd("evt-1", "ORDER-1", List.of("SKU-1")));

        // replay deduct + a late release must not change anything
        deductUc.execute(new DeductStockCmd("evt-1", "ORDER-1", List.of("SKU-1")));
        releaseUc.execute(new ReleaseStockCmd("ORDER-1"));

        assertEquals(6, repository.store.get("SKU-1").available());
        assertEquals(0, repository.store.get("SKU-1").reserved());
    }
}
