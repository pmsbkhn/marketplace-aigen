package vn.marketplace.inventory.application.stock;

/** Use-case port: permanently deduct reserved stock when an order completes (from {@code OrderCompleted}). */
public interface DeductStock {
    void execute(DeductStockCmd cmd);
}
