package vn.marketplace.catalog.application.product;

import java.util.List;

/** Use-case port: supply price snapshots from the DB source of truth (read-only). */
public interface GetPrice {
    List<SkuPriceView> execute(GetPriceCmd cmd);
}
