package vn.marketplace.inventory.application.stock;

import java.util.List;

import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Releases the hold for an order, returning units to {@code available}. Idempotent: an already
 * released/consumed reservation is a no-op (so {@code success=true} even on replay, and consumed
 * stock is never returned). State-writing → {@link EventPublishHandler}.
 */
public class ReleaseStockUc implements ReleaseStock {

    private final StockRepository stockRepository;

    public ReleaseStockUc(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(ReleaseStockCmd cmd) {
        String orderRef = cmd.reservationId();
        List<Stock> stocks = stockRepository.findByOrderRef(orderRef);
        for (Stock stock : stocks) {
            stock.releaseReservation(orderRef);
            stockRepository.save(stock);
        }
    }
}
