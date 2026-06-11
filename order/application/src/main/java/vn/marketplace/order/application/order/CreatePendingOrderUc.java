package vn.marketplace.order.application.order;

import java.util.List;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.OrderItemInput;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.ShippingAddressInput;
import vn.marketplace.order.domain.orderlifecycle.BuyerId;
import vn.marketplace.order.domain.orderlifecycle.MerchantId;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/**
 * Creates a PENDING order. Idempotent by {@code checkoutRef}: a duplicate request returns the existing
 * order rather than creating a second one. State-writing → {@link EventPublishHandler} (no event is
 * raised on creation, but the outbox proxy wraps every state-changing use-case — fitness rule).
 */
public class CreatePendingOrderUc implements CreatePendingOrder {

    private final Repository<Order> orderRepository;

    public CreatePendingOrderUc(Repository<Order> orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public OrderIdView execute(CreatePendingOrderCmd cmd) {
        List<Order> existing = orderRepository.findBy(Criteria.where("checkoutRef").eq(cmd.checkoutRef()));
        if (!existing.isEmpty()) {
            Order order = existing.get(0);
            return new OrderIdView(order.id().value(), order.status().name(), order.totalAmount().amount());
        }

        ShippingAddressInput a = cmd.shippingAddress();
        ShippingAddress address = new ShippingAddress(a.fullName(), a.phone(), a.addressLine(),
                a.ward(), a.district(), a.city());

        List<Order.NewItem> items = cmd.items().stream()
                .map(this::toNewItem)
                .toList();

        Order order = Order.createPending(new OrderId(UUID.randomUUID().toString()), cmd.checkoutRef(),
                new BuyerId(cmd.buyerId()), new MerchantId(cmd.merchantId()), address, items, cmd.buyerId());
        orderRepository.save(order);
        return new OrderIdView(order.id().value(), order.status().name(), order.totalAmount().amount());
    }

    private Order.NewItem toNewItem(OrderItemInput i) {
        return new Order.NewItem(UUID.randomUUID().toString(), i.productId(), i.variantId(), i.skuCode(),
                i.productName(), i.priceSnapshot(), i.currency(), i.quantity());
    }
}
