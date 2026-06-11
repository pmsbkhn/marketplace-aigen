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
import tech.vsf.ptnt.msfw.domain.core.criteria.Condition;
import tech.vsf.ptnt.msfw.domain.core.criteria.MatchAll;
import tech.vsf.ptnt.msfw.domain.core.criteria.Operator;
import vn.marketplace.payment.domain.payment.management.EscrowHold;
import vn.marketplace.payment.domain.payment.management.Payment;

/** Hand-written in-memory {@link PaymentRepository} fake (preferred over Mockito for new JDKs). */
class InMemoryPaymentRepository implements PaymentRepository {

    final Map<String, Payment> store = new LinkedHashMap<>();
    final List<Payment> saved = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Payment aggregate) {
        if (aggregate._id() == null) {
            aggregate.set_id(sequence.incrementAndGet());
        }
        saved.add(aggregate);
        store.put(aggregate.id().value(), aggregate);
    }

    @Override
    public <U extends Identity<?>> Optional<Payment> findById(U id) {
        return Optional.ofNullable(store.get(String.valueOf(id.value())));
    }

    @Override
    public List<Payment> findBy(Criteria criteria) {
        if (criteria instanceof MatchAll) {
            return new ArrayList<>(store.values());
        }
        if (criteria instanceof Condition c && c.operator() == Operator.EQ) {
            return switch (c.attribute()) {
                case "orderRef" -> store.values().stream()
                        .filter(p -> c.singleValue().equals(p.orderRef()))
                        .toList();
                case "gatewayTxnId" -> store.values().stream()
                        .filter(p -> c.singleValue().equals(p.gatewayTxnId()))
                        .toList();
                default -> throw new UnsupportedOperationException(
                        "Attribute not supported by this fake: " + c.attribute());
            };
        }
        throw new UnsupportedOperationException("Criteria not supported by this fake: " + criteria);
    }

    /** Mirrors the adapter's holds-collection join (beyond the Criteria translator). */
    @Override
    public Optional<Payment> findByHoldOrderId(String orderId) {
        return store.values().stream()
                .filter(p -> p.holds().stream().map(EscrowHold::orderId).anyMatch(orderId::equals))
                .findFirst();
    }

    @Override
    public PagedSearchResult<Payment> findBy(Criteria criteria, Pagination pagination) {
        List<Payment> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Payment> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        store.remove(String.valueOf(id.value()));
    }
}
