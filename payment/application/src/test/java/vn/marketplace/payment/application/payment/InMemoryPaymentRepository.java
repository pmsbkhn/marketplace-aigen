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
import vn.marketplace.payment.domain.payment.management.EscrowHold;
import vn.marketplace.payment.domain.payment.management.Payment;

/** Hand-written in-memory {@code Repository<Payment>} fake (preferred over Mockito for new JDKs). */
class InMemoryPaymentRepository implements Repository<Payment> {

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
        PaymentCriteria c = (PaymentCriteria) criteria;
        return store.values().stream()
                .filter(p -> c.orderRef() == null || c.orderRef().equals(p.orderRef()))
                .filter(p -> c.gatewayTxnId() == null || c.gatewayTxnId().equals(p.gatewayTxnId()))
                .filter(p -> c.orderId() == null || p.holds().stream()
                        .map(EscrowHold::orderId)
                        .anyMatch(c.orderId()::equals))
                .toList();
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
