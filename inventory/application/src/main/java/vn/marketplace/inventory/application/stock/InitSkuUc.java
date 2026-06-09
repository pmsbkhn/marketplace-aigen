package vn.marketplace.inventory.application.stock;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.inventory.domain.stock.MerchantId;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Initialises each SKU at quantity 0. Idempotent by SKU: a SKU that already has a stock row is left
 * untouched (so a replayed {@code ProductCreated} never resets existing stock — SAD ADR-3).
 * State-writing → {@link EventPublishHandler} (Event-Publish fitness function).
 */
public class InitSkuUc implements InitSku {

    private final Repository<Stock> stockRepository;

    public InitSkuUc(Repository<Stock> stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public void execute(InitSkuCmd cmd) {
        MerchantId merchantId = new MerchantId(cmd.merchantId());
        for (String code : cmd.skuCodes()) {
            SkuCode sku = new SkuCode(code);
            if (stockRepository.findById(sku).isEmpty()) {
                stockRepository.save(Stock.initFor(sku, merchantId));
            }
        }
    }
}
