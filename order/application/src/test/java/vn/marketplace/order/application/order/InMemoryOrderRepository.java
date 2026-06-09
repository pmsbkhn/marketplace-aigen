package vn.marketplace.order.application.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.order.domain.orderlifecycle.management.Order;

/** Hand-written in-memory {@code Repository<Order>} fake (preferred over Mockito for new JDKs). */
class InMemoryOrderRepository implements Repository<Order> {

    final Map<String, Order> store = new LinkedHashMap<>();
    final List<Order> saved = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Order aggregate) {
        if (aggregate._id() == null) {
            aggregate.set_id(sequence.incrementAndGet());
        }
        saved.add(aggregate);
        store.put(aggregate.id().value(), aggregate);
    }

    @Override
    public <U extends Identity<?>> Optional<Order> findById(U id) {
        return Optional.ofNullable(store.get(String.valueOf(id.value())));
    }

    @Override
    public List<Order> findBy(Criteria criteria) {
        OrderCriteria c = (OrderCriteria) criteria;
        return store.values().stream()
                .filter(o -> c.checkoutRef() == null || c.checkoutRef().equals(o.checkoutRef()))
                .filter(o -> c.buyerId() == null || c.buyerId().equals(o.buyerId().value()))
                .filter(o -> c.merchantId() == null || c.merchantId().equals(o.merchantId().value()))
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
        store.remove(String.valueOf(id.value()));
    }
}
