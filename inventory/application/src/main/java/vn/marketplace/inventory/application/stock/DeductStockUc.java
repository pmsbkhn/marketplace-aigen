package vn.marketplace.inventory.application.stock;

import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Permanently consumes the held units for a completed order. Idempotent: consuming an already-consumed
 * (or missing) reservation is a no-op — a replayed {@code OrderCompleted} does not double-deduct.
 * A missing reservation is tolerated (no crash). State-writing → {@link EventPublishHandler}.
 */
public class DeductStockUc implements DeductStock {

    private final Repository<Stock> stockRepository;

    public DeductStockUc(Repository<Stock> stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(DeductStockCmd cmd) {
        for (String code : cmd.skus()) {
            Optional<Stock> stock = stockRepository.findById(new SkuCode(code));
            if (stock.isEmpty()) {
                continue; // unknown SKU — tolerate (anomaly handled by alerting in production)
            }
            Stock s = stock.get();
            s.consumeReservation(cmd.orderId());
            stockRepository.save(s);
        }
    }
}
