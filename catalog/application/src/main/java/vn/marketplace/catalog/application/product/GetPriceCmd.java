package vn.marketplace.catalog.application.product;

import java.util.List;

/** Request a price snapshot for up to N sku codes. */
public record GetPriceCmd(List<String> skuCodes) {
}
