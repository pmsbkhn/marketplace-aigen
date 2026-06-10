package vn.marketplace.checkout.application.checkout;

import java.util.List;

/**
 * Outbound port to Catalog ({@code Catalog.GetPrice} — gRPC in production, REST stand-in here).
 * The returned prices are the ONLY price source for the saga (Price Authority Invariant).
 */
public interface CatalogPort {

    List<SkuPriceDto> fetchPrices(List<String> skuCodes);

    /** {@code active=false} when the SKU is missing or its product is not ACTIVE. */
    record SkuPriceDto(String skuCode, long priceAmount, String currency, String merchantId,
                       boolean active) {
    }
}
