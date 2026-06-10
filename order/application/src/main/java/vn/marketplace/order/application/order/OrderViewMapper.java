package vn.marketplace.order.application.order;

import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/** Maps the {@link Order} aggregate to its detailed {@link OrderView} read model. */
final class OrderViewMapper {

    private OrderViewMapper() {
    }

    static OrderView toDetail(Order order) {
        ShippingAddress a = order.shippingAddress();
        OrderView.AddressView address = a == null ? null
                : new OrderView.AddressView(a.fullName(), a.phone(), a.addressLine(), a.ward(), a.district(), a.city());

        var items = order.items().stream()
                .map(i -> new OrderView.ItemView(i.skuCode(), i.productName(),
                        i.priceSnapshot().amount(), i.qty(), i.subtotal().amount()))
                .toList();

        var history = order.statusHistory().stream()
                .map(h -> new OrderView.HistoryView(
                        h.fromStatus() == null ? null : h.fromStatus().name(),
                        h.toStatus().name(), h.triggeredBy(), h.reason(), h.timestamp()))
                .toList();

        return new OrderView(order.id().value(), order.checkoutRef(), order.buyerId().value(),
                order.merchantId().value(), order.status().name(), order.totalAmount().amount(),
                order.totalAmount().currency().name(), order.trackingNumber(), order.cancelReason(),
                address, items, history, order.createdAt(), order.updatedAt());
    }
}
