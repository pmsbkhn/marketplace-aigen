package vn.marketplace.catalog.application.product;

/** Use-case port: read a single product, honouring tenant scope + moderation visibility. */
public interface GetProduct {
    ProductView execute(GetProductCmd cmd);
}
