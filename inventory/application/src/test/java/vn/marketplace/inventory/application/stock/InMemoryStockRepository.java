package vn.marketplace.inventory.application.stock;

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
import tech.vsf.ptnt.msfw.domain.core.criteria.Condition;
import tech.vsf.ptnt.msfw.domain.core.criteria.MatchAll;
import tech.vsf.ptnt.msfw.domain.core.criteria.Operator;
import vn.marketplace.inventory.domain.stock.management.Stock;

/** Hand-written in-memory {@link StockRepository} fake (preferred over Mockito for new JDKs). */
class InMemoryStockRepository implements StockRepository {

    final Map<String, Stock> store = new LinkedHashMap<>();
    final List<Stock> saved = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Stock aggregate) {
        if (aggregate._id() == null) {
            aggregate.set_id(sequence.incrementAndGet());
        }
        saved.add(aggregate);
        store.put(aggregate.sku().value(), aggregate);
    }

    @Override
    public <U extends Identity<?>> Optional<Stock> findById(U id) {
        return Optional.ofNullable(store.get(String.valueOf(id.value())));
    }

    @Override
    public List<Stock> findBy(Criteria criteria) {
        if (criteria instanceof MatchAll) {
            return new ArrayList<>(store.values());
        }
        if (criteria instanceof Condition c && "sku".equals(c.attribute()) && c.operator() == Operator.IN) {
            return store.values().stream()
                    .filter(s -> c.values().contains(s.sku().value()))
                    .toList();
        }
        throw new UnsupportedOperationException("Criteria not supported by this fake: " + criteria);
    }

    @Override
    public List<Stock> findByOrderRef(String orderRef) {
        return store.values().stream()
                .filter(s -> s.reservations().stream().anyMatch(r -> r.orderRef().equals(orderRef)))
                .toList();
    }

    @Override
    public PagedSearchResult<Stock> findBy(Criteria criteria, Pagination pagination) {
        List<Stock> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Stock> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        store.remove(String.valueOf(id.value()));
    }
}
