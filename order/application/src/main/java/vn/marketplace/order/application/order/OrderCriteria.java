package vn.marketplace.order.application.order;

import tech.vsf.ptnt.msfw.domain.core.Criteria;

/**
 * Lookup criteria for the {@code Repository<Order>} port. {@code checkoutRef} is used for
 * create-idempotency; {@code buyerId}/{@code merchantId}/{@code status} drive tenant-scoped listing.
 */
public record OrderCriteria(String checkoutRef, String buyerId, String merchantId, String status)
        implements Criteria {

    public static OrderCriteria byCheckoutRef(String checkoutRef) {
        return new OrderCriteria(checkoutRef, null, null, null);
    }
}
