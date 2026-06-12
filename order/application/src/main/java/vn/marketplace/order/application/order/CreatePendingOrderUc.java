package vn.marketplace.order.application.order;

import java.util.List;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.OrderItemInput;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.ShippingAddressInput;
import vn.marketplace.order.domain.orderlifecycle.BuyerId;
import vn.marketplace.order.domain.orderlifecycle.MerchantId;
import vn.marketplace.order.domain.orderlifecycle.OrderId;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.orderlifecycle.management.Order;
import vn.marketplace.order.domain.orderlifecycle.management.OrderPendingTimedOut;

/**
 * Creates a PENDING order. Idempotent by {@code checkoutRef}: a duplicate request returns the existing
 * order rather than creating a second one. Every creation also arms the FR13 payment deadline: an
 * {@link OrderPendingTimedOut} delayed event ({@code deliverAfter} = now + pending-expiry TTL) goes
 * into the same outbox transaction; when it comes back, {@link ExpirePendingOrderUc} cancels the
 * order iff still PENDING. State-writing → {@link EventPublishHandler}.
 */
public class CreatePendingOrderUc implements CreatePendingOrder {

    /** Techspec §4.4 default for {@code order.pending_expiry_min}. */
    public static final int DEFAULT_PENDING_EXPIRY_MINUTES = 30;

    private final Repository<Order> orderRepository;
    private final int pendingExpiryMinutes;

    public CreatePendingOrderUc(Repository<Order> orderRepository) {
        this(orderRepository, DEFAULT_PENDING_EXPIRY_MINUTES);
    }

    public CreatePendingOrderUc(Repository<Order> orderRepository, int pendingExpiryMinutes) {
        this.orderRepository = orderRepository;
        this.pendingExpiryMinutes = pendingExpiryMinutes;
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

        // Arm the payment deadline: held by the outbox until due, then delivered back to this
        // service. Only on a real creation — the idempotent replay above must not re-arm it.
        DTime now = DTime.now();
        DomainEventPublisher.publish(new OrderPendingTimedOut(order.id().value(), now,
                DTime.of(now.value().plusMinutes(pendingExpiryMinutes))));

        return new OrderIdView(order.id().value(), order.status().name(), order.totalAmount().amount());
    }

    private Order.NewItem toNewItem(OrderItemInput i) {
        return new Order.NewItem(UUID.randomUUID().toString(), i.productId(), i.variantId(), i.skuCode(),
                i.productName(), i.priceSnapshot(), i.currency(), i.quantity());
    }
}
