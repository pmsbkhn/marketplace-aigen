package vn.marketplace.inventory.application.stock;

/** Use-case port: a Merchant sets the absolute available quantity for one of their own SKUs. */
public interface UpdateMerchantStock {
    StockView execute(UpdateMerchantStockCmd cmd);
}
