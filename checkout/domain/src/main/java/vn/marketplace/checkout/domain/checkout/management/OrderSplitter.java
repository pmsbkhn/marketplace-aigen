package vn.marketplace.checkout.domain.checkout.management;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service: splits the priced cart into one {@link MerchantGroup} per merchant (FR3 — each
 * group becomes one pending order, while the escrow covers the whole cart). Pure logic, no I/O;
 * group order follows first appearance in the cart (deterministic).
 */
public class OrderSplitter {

    public List<MerchantGroup> split(CartSnapshot cart) {
        Map<String, List<LineItem>> byMerchant = new LinkedHashMap<>();
        for (LineItem item : cart.items()) {
            byMerchant.computeIfAbsent(item.merchantId(), k -> new ArrayList<>()).add(item);
        }
        return byMerchant.entrySet().stream()
                .map(e -> new MerchantGroup(e.getKey(), e.getValue()))
                .toList();
    }
}
