package vn.marketplace.inventory.application.stock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.inventory.application.stock.ReserveStockCmd.StockItemInput;
import vn.marketplace.inventory.application.stock.ReserveStockResult.ItemReserveDetail;
import vn.marketplace.inventory.domain.shared.InventoryDomainException;
import vn.marketplace.inventory.domain.shared.InventoryErrorCode;
import vn.marketplace.inventory.domain.stock.ReservationId;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Holds stock for an order, <strong>all-or-nothing</strong>: if any SKU is short, nothing is reserved
 * and {@code allReserved=false} is returned with per-SKU detail (the saga treats this as failure).
 * Idempotent by {@code orderRef} — a replay reuses the existing hold. State-writing →
 * {@link EventPublishHandler}.
 */
public class ReserveStockUc implements ReserveStock {

    private static final int RESERVE_TTL_MINUTES = 15;

    private final Repository<Stock> stockRepository;

    public ReserveStockUc(Repository<Stock> stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public ReserveStockResult execute(ReserveStockCmd cmd) {
        // Load every requested SKU up front; a missing stock row is a data error (unknown SKU).
        Map<StockItemInput, Stock> loaded = new LinkedHashMap<>();
        for (StockItemInput item : cmd.items()) {
            Stock stock = stockRepository.findById(new SkuCode(item.sku()))
                    .orElseThrow(() -> new InventoryDomainException(
                            InventoryErrorCode.STOCK_NOT_FOUND, "Stock not found for SKU: " + item.sku()));
            loaded.put(item, stock);
        }

        // Pass 1: can every item be satisfied? An item already held for this orderRef counts as OK (idempotent replay).
        boolean allOk = true;
        List<ItemReserveDetail> details = new ArrayList<>();
        for (Map.Entry<StockItemInput, Stock> e : loaded.entrySet()) {
            StockItemInput item = e.getKey();
            Stock stock = e.getValue();
            boolean alreadyHeld = stock.heldReservationFor(cmd.orderRef()).isPresent();
            boolean ok = alreadyHeld || stock.available() >= item.quantity();
            allOk &= ok;
            details.add(new ItemReserveDetail(item.sku(),
                    ok ? ItemReserveDetail.OK : ItemReserveDetail.INSUFFICIENT_STOCK, stock.available()));
        }

        if (!allOk) {
            return new ReserveStockResult(cmd.orderRef(), false, details);
        }

        // Pass 2: apply (reserve() is idempotent per orderRef).
        for (Map.Entry<StockItemInput, Stock> e : loaded.entrySet()) {
            Stock stock = e.getValue();
            stock.reserve(e.getKey().quantity(), cmd.orderRef(),
                    new ReservationId(UUID.randomUUID().toString()),
                    LocalDateTime.now().plusMinutes(RESERVE_TTL_MINUTES));
            stockRepository.save(stock);
        }
        return new ReserveStockResult(cmd.orderRef(), true, details);
    }
}
