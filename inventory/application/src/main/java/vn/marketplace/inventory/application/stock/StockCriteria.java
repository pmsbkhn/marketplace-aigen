package vn.marketplace.inventory.application.stock;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Criteria;

/**
 * Lookup criteria for the {@code Repository<Stock>} port. Exactly one selector is set:
 * <ul>
 *   <li>{@code skuCodes} — load stock rows for these SKUs (reserve / deduct / get-level).</li>
 *   <li>{@code orderRef} — load stock rows that hold a reservation for this order (release).</li>
 * </ul>
 */
public record StockCriteria(List<String> skuCodes, String orderRef) implements Criteria {

    public static StockCriteria bySkus(List<String> skuCodes) {
        return new StockCriteria(skuCodes, null);
    }

    public static StockCriteria byOrderRef(String orderRef) {
        return new StockCriteria(null, orderRef);
    }
}
