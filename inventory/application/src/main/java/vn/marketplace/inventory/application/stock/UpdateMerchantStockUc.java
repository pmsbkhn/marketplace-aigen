package vn.marketplace.inventory.application.stock;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.inventory.domain.shared.InventoryDomainException;
import vn.marketplace.inventory.domain.shared.InventoryErrorCode;
import vn.marketplace.inventory.domain.stock.MerchantId;
import vn.marketplace.inventory.domain.stock.SkuCode;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Sets a SKU's absolute available quantity. Tenant scope (owning merchant only) and the qty ≥ 0 rule
 * are enforced in the domain ({@code TENANT_MISMATCH} / {@code NEGATIVE_STOCK}). State-writing →
 * {@link EventPublishHandler}.
 */
public class UpdateMerchantStockUc implements UpdateMerchantStock {

    private final Repository<Stock> stockRepository;

    public UpdateMerchantStockUc(Repository<Stock> stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public StockView execute(UpdateMerchantStockCmd cmd) {
        Stock stock = stockRepository.findById(new SkuCode(cmd.sku()))
                .orElseThrow(() -> new InventoryDomainException(InventoryErrorCode.STOCK_NOT_FOUND));

        stock.changeAvailable(cmd.available(), new MerchantId(cmd.callerMerchantId()));
        stockRepository.save(stock);

        return new StockView(stock.sku().value(), stock.available(), stock.reserved(),
                stock.merchantId().value(), stock.version());
    }
}
