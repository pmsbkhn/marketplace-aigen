package vn.marketplace.catalog.application.product;

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
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.management.Product;

/** Hand-written in-memory {@code Repository<Product>} fake (preferred over Mockito for new JDKs). */
class InMemoryProductRepository implements Repository<Product> {

    final Map<String, Product> store = new LinkedHashMap<>();
    final List<Product> saved = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Product aggregate) {
        if (aggregate._id() == null) {
            aggregate.set_id(sequence.incrementAndGet());
        }
        saved.add(aggregate);
        store.put(aggregate.id().value(), aggregate);
    }

    @Override
    public <U extends Identity<?>> Optional<Product> findById(U id) {
        return Optional.ofNullable(store.get(String.valueOf(id.value())));
    }

    @Override
    public List<Product> findBy(Criteria criteria) {
        ProductCriteria c = (ProductCriteria) criteria;
        if (c.skuCodes() != null) {
            List<Product> result = new ArrayList<>();
            for (Product p : store.values()) {
                for (String code : c.skuCodes()) {
                    if (p.findSku(new SkuCode(code)).isPresent()) {
                        result.add(p);
                        break;
                    }
                }
            }
            return result;
        }
        if (Boolean.TRUE.equals(c.activeOnly())) {
            return store.values().stream().filter(Product::isActive).toList();
        }
        return new ArrayList<>(store.values());
    }

    @Override
    public PagedSearchResult<Product> findBy(Criteria criteria, Pagination pagination) {
        List<Product> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Product> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        store.remove(String.valueOf(id.value()));
    }
}
