package vn.marketplace.inventory.application.stock;

/** Use-case port: release a previously held reservation (saga compensation / TTL expiry). */
public interface ReleaseStock {
    void execute(ReleaseStockCmd cmd);
}
