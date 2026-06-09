package vn.marketplace.catalog.application.product;

/** Use-case port: a Merchant creates a product (starts PENDING). */
public interface CreateProduct {
    ProductIdView execute(CreateProductCmd cmd);
}
