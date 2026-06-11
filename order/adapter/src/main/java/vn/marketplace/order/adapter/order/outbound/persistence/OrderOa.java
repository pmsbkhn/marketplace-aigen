package vn.marketplace.order.adapter.order.outbound.persistence;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.springcore.persistence.AbstractMementoJpaOa;
import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderEntity;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderItemEntity;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderStatusHistoryEntity;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/**
 * Outbound persistence adapter: maps the {@link Order} aggregate ↔ JPA entities at the
 * {@link Order.Memento} level via msfw's {@code AbstractMementoJpaOa}, which owns the plumbing
 * (surrogate-key threading, identity lookups, {@code Criteria} → Specification translation, real
 * DB paging). {@code @Transactional} keeps the session open while the lazy item/history collections
 * are read during reconstruction.
 *
 * <p>On save both child collections are rebuilt from the memento ({@code orphanRemoval} syncs the
 * tables): items are value-immutable so the rebuild is a no-op state-wise, and status history is
 * append-only so the rebuild preserves its ordering. {@link #save} also upserts by {@code orderId}:
 * a fresh aggregate ({@code _id == null}) whose order id already has a row updates that row instead
 * of violating the unique index, preserving the legacy adapter's semantics.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class OrderOa extends AbstractMementoJpaOa<Order, Order.Memento, OrderEntity> {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    protected JpaOaRepository<OrderEntity> jpa() {
        return orderJpaRepository;
    }

    @Override
    public void save(Order aggregate) {
        if (aggregate._id() == null) {
            findEntity(aggregate.id()).ifPresent(e -> aggregate.set_id(e.getId()));
        }
        super.save(aggregate);
    }

    @Override
    protected OrderEntity fromMemento(Order.Memento m) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(m.orderId());
        entity.setCheckoutRef(m.checkoutRef());
        entity.setBuyerId(m.buyerId());
        entity.setMerchantId(m.merchantId());
        entity.setStatus(m.status());
        entity.setTotalAmount(m.totalAmount());
        entity.setCurrency(m.currency());
        ShippingAddress a = m.shippingAddress();
        if (a != null) {
            entity.setAddrFullName(a.fullName());
            entity.setAddrPhone(a.phone());
            entity.setAddrLine(a.addressLine());
            entity.setAddrWard(a.ward());
            entity.setAddrDistrict(a.district());
            entity.setAddrCity(a.city());
        }
        entity.setTrackingNumber(m.trackingNumber());
        entity.setCancelReason(m.cancelReason());
        entity.setCreatedAt(m.createdAt());
        entity.setUpdatedAt(m.updatedAt());

        for (Order.OrderItemMemento im : m.items()) {
            OrderItemEntity ie = new OrderItemEntity();
            ie.setOrderItemId(im.orderItemId());
            ie.setProductId(im.productId());
            ie.setVariantId(im.variantId());
            ie.setSkuCode(im.skuCode());
            ie.setProductName(im.productName());
            ie.setPriceSnapshot(im.priceAmount());
            ie.setCurrency(im.currency());
            ie.setQty(im.qty());
            entity.getItems().add(ie);
        }

        for (Order.StatusHistoryMemento hm : m.statusHistory()) {
            OrderStatusHistoryEntity he = new OrderStatusHistoryEntity();
            he.setHistoryId(hm.historyId());
            he.setFromStatus(hm.fromStatus());
            he.setToStatus(hm.toStatus());
            he.setTriggeredBy(hm.triggeredBy());
            he.setReason(hm.reason());
            he.setTimestamp(hm.timestamp());
            entity.getStatusHistory().add(he);
        }
        return entity;
    }

    @Override
    protected Order.Memento toMemento(OrderEntity e) {
        List<Order.OrderItemMemento> items = new ArrayList<>();
        for (OrderItemEntity ie : e.getItems()) {
            items.add(new Order.OrderItemMemento(ie.getOrderItemId(), ie.getProductId(), ie.getVariantId(),
                    ie.getSkuCode(), ie.getProductName(), ie.getPriceSnapshot(), ie.getCurrency(), ie.getQty()));
        }
        List<Order.StatusHistoryMemento> history = new ArrayList<>();
        for (OrderStatusHistoryEntity he : e.getStatusHistory()) {
            history.add(new Order.StatusHistoryMemento(he.getHistoryId(), he.getFromStatus(), he.getToStatus(),
                    he.getTriggeredBy(), he.getReason(), he.getTimestamp()));
        }
        ShippingAddress address = new ShippingAddress(e.getAddrFullName(), e.getAddrPhone(), e.getAddrLine(),
                e.getAddrWard(), e.getAddrDistrict(), e.getAddrCity());

        return new Order.Memento(e.getId(), e.getOrderId(), e.getCheckoutRef(), e.getBuyerId(),
                e.getMerchantId(), e.getStatus(), e.getTotalAmount(), e.getCurrency(), address,
                e.getTrackingNumber(), e.getCancelReason(), e.getCreatedAt(), e.getUpdatedAt(), items, history);
    }

    @Override
    protected Order restore(Order.Memento memento) {
        return Order.restore(memento);
    }

    @Override
    protected Criteria identityCriteria(Identity<?> id) {
        return Criteria.where("orderId").eq(String.valueOf(id.value()));
    }
}
