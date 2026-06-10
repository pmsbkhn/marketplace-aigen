package vn.marketplace.checkout.domain.checkout.management;

import java.util.List;

import tech.vsf.ptnt.msfw.domain.DomainValue;
import vn.marketplace.checkout.domain.checkout.Money;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

/**
 * The priced cart at the moment of checkout — immutable value object built exclusively from
 * Catalog prices. {@code grandTotal = Σ unitPrice × quantity} across all lines.
 */
public record CartSnapshot(List<LineItem> items) implements DomainValue {

    public CartSnapshot {
        if (items == null || items.isEmpty()) {
            throw new CheckoutDomainException(CheckoutErrorCode.EMPTY_CART);
        }
        items = List.copyOf(items);
    }

    public Money grandTotal() {
        Money total = Money.of(0, items.get(0).unitPrice().currency());
        for (LineItem item : items) {
            total = total.plus(item.subtotal());
        }
        return total;
    }
}
