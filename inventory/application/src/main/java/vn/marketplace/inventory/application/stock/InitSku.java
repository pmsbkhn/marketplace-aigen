package vn.marketplace.inventory.application.stock;

/** Use-case port: initialise stock (=0) for a product's SKUs — driven by Catalog's {@code ProductCreated}. */
public interface InitSku {
    void execute(InitSkuCmd cmd);
}
