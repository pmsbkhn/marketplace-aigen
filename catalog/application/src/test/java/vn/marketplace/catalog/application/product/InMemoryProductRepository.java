package vn.marketplace.catalog.application.product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import tech.vsf.ptnt.msfw.domain.core.criteria.MatchAll;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.management.Product;

/** Hand-written in-memory {@link ProductRepository} fake (preferred over Mockito for new JDKs). */
class InMemoryProductRepository implements ProductRepository {

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
    public List<Product> findBySkuCodes(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }
        List<Product> result = new ArrayList<>();
        for (Product p : store.values()) {
            for (String code : skuCodes) {
                if (p.findSku(new SkuCode(code)).isPresent()) {
                    result.add(p);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public PagedSearchResult<Product> searchActive(String text,
                                                   String categoryId,
                                                   String brandId,
                                                   Long priceMin,
                                                   Long priceMax,
                                                   Pagination pagination) {
        List<Product> matched = store.values().stream()
                .filter(Product::isActive)
                .filter(p -> matchesSearch(p, text, categoryId, brandId, priceMin, priceMax))
                .toList();
        return slice(matched, pagination);
    }

    private boolean matchesSearch(Product p, String text, String categoryId, String brandId,
                                  Long priceMin, Long priceMax) {
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase(Locale.ROOT);
            String hay = (p.name() + " " + (p.description() == null ? "" : p.description()))
                    .toLowerCase(Locale.ROOT);
            if (!hay.contains(needle)) {
                return false;
            }
        }
        if (categoryId != null && (p.categoryId() == null || !categoryId.equals(p.categoryId().value()))) {
            return false;
        }
        if (brandId != null && (p.brandId() == null || !brandId.equals(p.brandId().value()))) {
            return false;
        }
        if (priceMin != null || priceMax != null) {
            boolean any = p.variants().stream()
                    .flatMap(v -> v.skus().stream())
                    .anyMatch(s -> {
                        long amt = s.price().amount();
                        boolean okMin = priceMin == null || amt >= priceMin;
                        boolean okMax = priceMax == null || amt <= priceMax;
                        return okMin && okMax;
                    });
            if (!any) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Product> findBy(Criteria criteria) {
        if (criteria instanceof MatchAll) {
            return new ArrayList<>(store.values());
        }
        throw new UnsupportedOperationException("Criteria not supported by this fake: " + criteria);
    }

    @Override
    public PagedSearchResult<Product> findBy(Criteria criteria, Pagination pagination) {
        return slice(findBy(criteria), pagination);
    }

    private PagedSearchResult<Product> slice(List<Product> all, Pagination pagination) {
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
