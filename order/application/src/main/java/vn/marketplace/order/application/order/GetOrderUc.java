package vn.marketplace.order.application.order;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.management.Order;
import vn.marketplace.order.domain.shared.Actor;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

/**
 * Reads an order, enforcing visibility:
 * <ul>
 *   <li>Admin: any order.</li>
 *   <li>Merchant: only their own orders.</li>
 *   <li>Buyer: only their own orders.</li>
 * </ul>
 * A non-owner gets {@code ORDER_NOT_FOUND} (404, not 403) to avoid leaking existence across tenants.
 * Read-only — no event handler.
 */
public class GetOrderUc implements GetOrder {

    private final Repository<Order> orderRepository;

    public GetOrderUc(Repository<Order> orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderView execute(GetOrderCmd cmd) {
        Order order = orderRepository.findById(new OrderId(cmd.orderId()))
                .orElseThrow(() -> new OrderDomainException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!isVisibleTo(order, cmd.caller())) {
            throw new OrderDomainException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return OrderViewMapper.toDetail(order);
    }

    private boolean isVisibleTo(Order order, Actor caller) {
        if (caller == null) {
            return false;
        }
        return switch (caller.role()) {
            case ADMIN -> true;
            case MERCHANT -> order.merchantId().value().equals(caller.id());
            case BUYER -> order.buyerId().value().equals(caller.id());
        };
    }
}
