package vn.marketplace.notification.application.delivery;

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
import tech.vsf.ptnt.msfw.domain.core.criteria.Condition;
import tech.vsf.ptnt.msfw.domain.core.criteria.MatchAll;
import tech.vsf.ptnt.msfw.domain.core.criteria.Operator;
import vn.marketplace.notification.domain.delivery.management.Notification;

/** Hand-written in-memory {@code Repository<Notification>} fake (preferred over Mockito for new JDKs). */
class InMemoryNotificationRepository implements Repository<Notification> {

    final Map<String, Notification> store = new LinkedHashMap<>();
    final List<Notification> saved = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Notification aggregate) {
        if (aggregate._id() == null) {
            aggregate.set_id(sequence.incrementAndGet());
        }
        saved.add(aggregate);
        store.put(aggregate.id().value(), aggregate);
    }

    @Override
    public <U extends Identity<?>> Optional<Notification> findById(U id) {
        return Optional.ofNullable(store.get(String.valueOf(id.value())));
    }

    @Override
    public List<Notification> findBy(Criteria criteria) {
        return store.values().stream()
                .filter(n -> matches(criteria, n))
                .toList();
    }

    /** Interprets the msfw Criteria DSL tree for the attributes the use-cases actually query. */
    private boolean matches(Criteria criteria, Notification n) {
        if (criteria instanceof MatchAll) {
            return true;
        }
        if (criteria instanceof Condition c
                && "idempotencyKey".equals(c.attribute()) && c.operator() == Operator.EQ) {
            return c.singleValue().equals(n.idempotencyKey());
        }
        throw new IllegalArgumentException("Unsupported criteria in fake: " + criteria);
    }

    @Override
    public PagedSearchResult<Notification> findBy(Criteria criteria, Pagination pagination) {
        List<Notification> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Notification> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        store.remove(String.valueOf(id.value()));
    }
}
