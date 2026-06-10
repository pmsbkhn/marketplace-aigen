package vn.marketplace.checkout.domain.checkout.management;

import java.util.List;
import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;
import vn.marketplace.checkout.domain.checkout.Money;

/** One merchant's slice of the cart — becomes exactly one pending order. */
public record MerchantGroup(String merchantId, List<LineItem> items) implements DomainValue {

    public MerchantGroup {
        Objects.requireNonNull(merchantId, "merchantId cannot be null");
        items = List.copyOf(items);
    }

    public Money subtotal() {
        Money total = Money.of(0, items.get(0).unitPrice().currency());
        for (LineItem item : items) {
            total = total.plus(item.subtotal());
        }
        return total;
    }
}
