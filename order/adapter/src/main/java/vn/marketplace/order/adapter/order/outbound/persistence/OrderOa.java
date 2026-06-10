package vn.marketplace.order.adapter.order.outbound.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderEntity;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderItemEntity;
import vn.marketplace.order.adapter.order.outbound.persistence.entity.OrderStatusHistoryEntity;
import vn.marketplace.order.application.order.OrderCriteria;
import vn.marketplace.order.domain.orderlifecycle.ShippingAddress;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/**
 * Outbound persistence adapter: maps the {@link Order} aggregate ↔ JPA entities via the aggregate
 * {@link Order.Memento}, implementing the msfw {@code Repository<Order>} port. {@code @Transactional}
 * keeps the session open while lazy item/history collections are read during reconstruction.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class OrderOa implements tech.vsf.ptnt.msfw.domain.core.Repository<Order> {

    private final OrderJpaRepository jpa;

    @Override
    public void save(Order aggregate) {
        Order.Memento m = aggregate.toMemento();
        OrderEntity entity = jpa.findByOrderId(m.orderId()).orElseGet(OrderEntity::new);
        boolean isNew = entity.getId() == null;

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

        // Items are immutable once created — only populate for a new order.
        if (isNew) {
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
        }

        // Status history is append-only — rebuild from the memento (orphanRemoval syncs the table).
        entity.getStatusHistory().clear();
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

        OrderEntity saved = jpa.save(entity);
        aggregate.set_id(saved.getId());
    }

    @Override
    public <U extends Identity<?>> Optional<Order> findById(U id) {
        return jpa.findByOrderId(String.valueOf(id.value())).map(this::toDomain);
    }

    @Override
    public List<Order> findBy(Criteria criteria) {
        OrderCriteria c = (OrderCriteria) criteria;
        List<OrderEntity> entities;
        if (c.checkoutRef() != null) {
            entities = jpa.findByCheckoutRef(c.checkoutRef()).map(List::of).orElseGet(List::of);
        } else if (c.buyerId() != null) {
            entities = jpa.findByBuyerId(c.buyerId());
        } else if (c.merchantId() != null) {
            entities = jpa.findByMerchantId(c.merchantId());
        } else {
            entities = jpa.findAll();
        }
        return entities.stream()
                .map(this::toDomain)
                .filter(o -> c.status() == null || c.status().equals(o.status().name()))
                .toList();
    }

    @Override
    public PagedSearchResult<Order> findBy(Criteria criteria, Pagination pagination) {
        List<Order> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Order> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        jpa.deleteByOrderId(String.valueOf(id.value()));
    }

    private Order toDomain(OrderEntity e) {
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

        Order.Memento m = new Order.Memento(e.getId(), e.getOrderId(), e.getCheckoutRef(), e.getBuyerId(),
                e.getMerchantId(), e.getStatus(), e.getTotalAmount(), e.getCurrency(), address,
                e.getTrackingNumber(), e.getCancelReason(), e.getCreatedAt(), e.getUpdatedAt(), items, history);
        return Order.fromMemento(m);
    }
}
