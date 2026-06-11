package vn.marketplace.payment.application.payment;

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
import vn.marketplace.payment.domain.payment.management.Settlement;

/** Hand-written in-memory {@code Repository<Settlement>} fake. */
class InMemorySettlementRepository implements Repository<Settlement> {

    final Map<String, Settlement> store = new LinkedHashMap<>();
    final List<Settlement> saved = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Settlement aggregate) {
        if (aggregate._id() == null) {
            aggregate.set_id(sequence.incrementAndGet());
        }
        saved.add(aggregate);
        store.put(aggregate.id().value(), aggregate);
    }

    @Override
    public <U extends Identity<?>> Optional<Settlement> findById(U id) {
        return Optional.ofNullable(store.get(String.valueOf(id.value())));
    }

    @Override
    public List<Settlement> findBy(Criteria criteria) {
        if (criteria instanceof MatchAll) {
            return new ArrayList<>(store.values());
        }
        if (criteria instanceof Condition c && c.operator() == Operator.EQ) {
            return switch (c.attribute()) {
                case "orderId" -> store.values().stream()
                        .filter(s -> c.singleValue().equals(s.orderId()))
                        .toList();
                case "merchantId" -> store.values().stream()
                        .filter(s -> c.singleValue().equals(s.merchantId().value()))
                        .toList();
                default -> throw new UnsupportedOperationException(
                        "Attribute not supported by this fake: " + c.attribute());
            };
        }
        throw new UnsupportedOperationException("Criteria not supported by this fake: " + criteria);
    }

    @Override
    public PagedSearchResult<Settlement> findBy(Criteria criteria, Pagination pagination) {
        List<Settlement> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Settlement> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        store.remove(String.valueOf(id.value()));
    }
}
