package vn.marketplace.inventory.application.stock;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.inventory.domain.stock.management.Stock;

/** Reads current stock levels for the requested SKUs. Read-only — no event handler. */
public class GetStockLevelUc implements GetStockLevel {

    private final Repository<Stock> stockRepository;

    public GetStockLevelUc(Repository<Stock> stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public List<StockLevelView> execute(GetStockLevelCmd cmd) {
        List<String> skuCodes = cmd.skuCodes();
        if (skuCodes != null && skuCodes.isEmpty()) {
            return List.of();
        }
        Criteria criteria = skuCodes == null ? Criteria.matchAll() : Criteria.where("sku").in(skuCodes);
        return stockRepository.findBy(criteria).stream()
                .map(s -> new StockLevelView(s.sku().value(), s.available(), s.reserved()))
                .toList();
    }
}
