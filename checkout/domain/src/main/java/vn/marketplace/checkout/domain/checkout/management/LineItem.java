package vn.marketplace.checkout.domain.checkout.management;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;
import vn.marketplace.checkout.domain.checkout.Money;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

/**
 * One cart line with its server-side price snapshot. The {@code unitPrice} comes from Catalog
 * (Price Authority Invariant) — there is no constructor path that accepts a client-supplied price.
 */
public record LineItem(String sku, int quantity, String merchantId, Money unitPrice)
        implements DomainValue {

    public LineItem {
        Objects.requireNonNull(sku, "sku cannot be null");
        Objects.requireNonNull(merchantId, "merchantId cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        if (quantity <= 0) {
            throw new CheckoutDomainException(CheckoutErrorCode.INVALID_QUANTITY);
        }
        if (unitPrice.amount() <= 0) {
            throw new CheckoutDomainException(CheckoutErrorCode.INVALID_PRICE,
                    "Catalog price for " + sku + " must be positive");
        }
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }
}
