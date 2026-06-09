package vn.marketplace.inventory.application.stock;

/** Use-case port: hold stock for an order (called by Checkout during the saga). */
public interface ReserveStock {
    ReserveStockResult execute(ReserveStockCmd cmd);
}
