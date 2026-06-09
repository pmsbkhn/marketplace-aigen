package vn.marketplace.catalog.application.product;

/** Use-case port: a Merchant changes the price of one of their product's SKUs (flow 5.4). */
public interface UpdateSkuPrice {
    SkuPriceView execute(UpdateSkuPriceCmd cmd);
}
