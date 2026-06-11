package vn.marketplace.order.application.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.domain.core.criteria.Condition;
import tech.vsf.ptnt.msfw.domain.core.criteria.Junction;
import tech.vsf.ptnt.msfw.domain.core.criteria.MatchAll;
import tech.vsf.ptnt.msfw.domain.core.criteria.Operator;
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
        return store.values().stream()
                .filter(o -> matches(criteria, o))
                .toList();
    }

    /** Interprets the msfw Criteria DSL tree the way the JPA translator would (EQ on root attributes). */
    private boolean matches(Criteria criteria, Order order) {
        if (criteria instanceof MatchAll) {
            return true;
        }
        if (criteria instanceof Junction junction) {
            return junction.logic() == Junction.Logic.AND
                    ? junction.parts().stream().allMatch(part -> matches(part, order))
                    : junction.parts().stream().anyMatch(part -> matches(part, order));
        }
        if (criteria instanceof Condition condition) {
            if (condition.operator() != Operator.EQ) {
                throw new IllegalArgumentException("Unsupported operator in fake: " + condition.operator());
            }
            return Objects.equals(condition.singleValue(), attributeOf(order, condition.attribute()));
        }
        throw new IllegalArgumentException("Unsupported criteria type: " + criteria.getClass().getName());
    }

    private Object attributeOf(Order order, String attribute) {
        return switch (attribute) {
            case "orderId" -> order.id().value();
            case "checkoutRef" -> order.checkoutRef();
            case "buyerId" -> order.buyerId().value();
            case "merchantId" -> order.merchantId().value();
            case "status" -> order.status().name();
            default -> throw new IllegalArgumentException("Unknown attribute: " + attribute);
        };
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
