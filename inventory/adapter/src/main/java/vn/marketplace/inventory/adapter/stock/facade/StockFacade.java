package vn.marketplace.inventory.adapter.stock.facade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.marketplace.inventory.application.stock.DeductStock;
import vn.marketplace.inventory.application.stock.DeductStockCmd;
import vn.marketplace.inventory.application.stock.GetStockLevel;
import vn.marketplace.inventory.application.stock.GetStockLevelCmd;
import vn.marketplace.inventory.application.stock.InitSku;
import vn.marketplace.inventory.application.stock.InitSkuCmd;
import vn.marketplace.inventory.application.stock.ReleaseStock;
import vn.marketplace.inventory.application.stock.ReleaseStockCmd;
import vn.marketplace.inventory.application.stock.ReserveStock;
import vn.marketplace.inventory.application.stock.ReserveStockCmd;
import vn.marketplace.inventory.application.stock.ReserveStockResult;
import vn.marketplace.inventory.application.stock.StockLevelView;
import vn.marketplace.inventory.application.stock.StockView;
import vn.marketplace.inventory.application.stock.UpdateMerchantStock;
import vn.marketplace.inventory.application.stock.UpdateMerchantStockCmd;

/** Transactional application boundary for the inventory use-cases. */
@Service
public class StockFacade {

    private final ReserveStock reserveStock;
    private final ReleaseStock releaseStock;
    private final DeductStock deductStock;
    private final InitSku initSku;
    private final GetStockLevel getStockLevel;
    private final UpdateMerchantStock updateMerchantStock;

    public StockFacade(ReserveStock reserveStock,
                       ReleaseStock releaseStock,
                       DeductStock deductStock,
                       InitSku initSku,
                       GetStockLevel getStockLevel,
                       UpdateMerchantStock updateMerchantStock) {
        this.reserveStock = reserveStock;
        this.releaseStock = releaseStock;
        this.deductStock = deductStock;
        this.initSku = initSku;
        this.getStockLevel = getStockLevel;
        this.updateMerchantStock = updateMerchantStock;
    }

    @Transactional
    public ReserveStockResult reserve(ReserveStockCmd cmd) {
        return reserveStock.execute(cmd);
    }

    @Transactional
    public void release(String reservationId) {
        releaseStock.execute(new ReleaseStockCmd(reservationId));
    }

    @Transactional
    public void deduct(DeductStockCmd cmd) {
        deductStock.execute(cmd);
    }

    @Transactional
    public void init(InitSkuCmd cmd) {
        initSku.execute(cmd);
    }

    @Transactional(readOnly = true)
    public List<StockLevelView> levels(List<String> skuCodes) {
        return getStockLevel.execute(new GetStockLevelCmd(skuCodes));
    }

    @Transactional
    public StockView updateMerchantStock(String sku, int available, String callerMerchantId) {
        return updateMerchantStock.execute(new UpdateMerchantStockCmd(sku, available, callerMerchantId));
    }
}
