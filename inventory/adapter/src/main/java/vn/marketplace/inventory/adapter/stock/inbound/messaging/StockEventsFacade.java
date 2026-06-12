package vn.marketplace.inventory.adapter.stock.inbound.messaging;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.vsf.ptnt.msfw.event.causation.EventCausation;
import vn.marketplace.inventory.application.stock.DeductStock;
import vn.marketplace.inventory.application.stock.DeductStockCmd;
import vn.marketplace.inventory.application.stock.InitSku;
import vn.marketplace.inventory.application.stock.InitSkuCmd;

/**
 * Event-subscriber boundary (tech spec: {@code event-subscriber}) — maps consumed events to the
 * stock use-cases. {@code eventId} for application-level idempotency comes from
 * {@link EventCausation} (msfw exposes the consumed event's id around the handler invocation).
 */
@Service
public class StockEventsFacade {

    private final InitSku initSku;
    private final DeductStock deductStock;

    public StockEventsFacade(InitSku initSku, DeductStock deductStock) {
        this.initSku = initSku;
        this.deductStock = deductStock;
    }

    /** {@code Catalog/ProductCreated} → initialise each SKU at quantity 0. */
    @Transactional
    public void onProductCreated(ProductCreatedData data) {
        initSku.execute(new InitSkuCmd(consumedEventId(), data.productId(), data.skuCodes(), data.merchantId()));
    }

    /** {@code Order/OrderCompleted} → permanently deduct the reserved stock. */
    @Transactional
    public void onOrderCompleted(OrderCompletedData data) {
        List<String> skus = data.items() == null ? List.of()
                : data.items().stream().map(OrderCompletedData.Line::sku).toList();
        deductStock.execute(new DeductStockCmd(consumedEventId(), data.orderId(), skus));
    }

    private static String consumedEventId() {
        return EventCausation.current().map(EventCausation.Causation::causationId).orElse(null);
    }
}
