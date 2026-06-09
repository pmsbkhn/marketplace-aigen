package vn.marketplace.catalog.adapter.product.management.outbound.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductEntity;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductImageEntity;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.SkuEntity;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.VariantEntity;
import vn.marketplace.catalog.application.product.ProductCriteria;
import vn.marketplace.catalog.domain.product.management.Product;

/**
 * Outbound persistence adapter: maps the {@link Product} aggregate ↔ JPA entities via the aggregate
 * {@link Product.Memento}. Implements the msfw {@code Repository<Product>} port. {@code @Transactional}
 * keeps the session open while lazy variant/sku collections are read during reconstruction.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class ProductOa implements tech.vsf.ptnt.msfw.domain.core.Repository<Product> {

    private final ProductJpaRepository jpa;

    @Override
    public void save(Product aggregate) {
        Product.Memento m = aggregate.toMemento();
        ProductEntity entity = jpa.findByProductId(m.productId()).orElseGet(ProductEntity::new);
        boolean isNew = entity.getId() == null;

        applyScalars(entity, m);
        if (isNew) {
            entity.setVariants(toVariantEntities(m.variants()));
            entity.setImages(toImageEntities(m.images()));
        }

        ProductEntity saved = jpa.save(entity);
        aggregate.set_id(saved.getId());
    }

    @Override
    public <U extends Identity<?>> Optional<Product> findById(U id) {
        return jpa.findByProductId(String.valueOf(id.value())).map(this::toDomain);
    }

    @Override
    public List<Product> findBy(Criteria criteria) {
        ProductCriteria c = (ProductCriteria) criteria;
        List<ProductEntity> entities;
        if (c.skuCodes() != null) {
            entities = c.skuCodes().isEmpty() ? List.of() : jpa.findBySkuCodes(c.skuCodes());
        } else if (Boolean.TRUE.equals(c.activeOnly())) {
            entities = jpa.findByStatus("ACTIVE");
        } else if (c.merchantId() != null) {
            entities = jpa.findAll().stream()
                    .filter(e -> c.merchantId().equals(e.getMerchantId()))
                    .toList();
        } else {
            entities = jpa.findAll();
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public PagedSearchResult<Product> findBy(Criteria criteria, Pagination pagination) {
        ProductCriteria c = (ProductCriteria) criteria;
        List<Product> matched = jpa.findByStatus("ACTIVE").stream()
                .map(this::toDomain)
                .filter(p -> matchesSearch(p, c))
                .toList();

        int from = Math.min(pagination.offset(), matched.size());
        int to = Math.min(from + pagination.size(), matched.size());
        List<Product> pageContent = new ArrayList<>(matched.subList(from, to));
        Optional<Integer> nextPage = to < matched.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(pageContent, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        jpa.deleteByProductId(String.valueOf(id.value()));
    }

    // ---- search filtering (DB stand-in for Elasticsearch) ----

    private boolean matchesSearch(Product p, ProductCriteria c) {
        if (c.text() != null && !c.text().isBlank()) {
            String needle = c.text().toLowerCase(Locale.ROOT);
            String hay = (p.name() + " " + (p.description() == null ? "" : p.description())).toLowerCase(Locale.ROOT);
            if (!hay.contains(needle)) {
                return false;
            }
        }
        if (c.categoryId() != null && (p.categoryId() == null || !c.categoryId().equals(p.categoryId().value()))) {
            return false;
        }
        if (c.brandId() != null && (p.brandId() == null || !c.brandId().equals(p.brandId().value()))) {
            return false;
        }
        if (c.priceMin() != null || c.priceMax() != null) {
            boolean any = p.variants().stream()
                    .flatMap(v -> v.skus().stream())
                    .anyMatch(s -> {
                        long amt = s.price().amount();
                        boolean okMin = c.priceMin() == null || amt >= c.priceMin();
                        boolean okMax = c.priceMax() == null || amt <= c.priceMax();
                        return okMin && okMax;
                    });
            if (!any) {
                return false;
            }
        }
        return true;
    }

    // ---- mapping ----

    private void applyScalars(ProductEntity e, Product.Memento m) {
        e.setProductId(m.productId());
        e.setMerchantId(m.merchantId());
        e.setName(m.name());
        e.setDescription(m.description());
        e.setStatus(m.status());
        e.setBrandId(m.brandId());
        e.setCategoryId(m.categoryId());
        e.setProductCreatedPublished(m.productCreatedPublished());
        e.setCreatedBy(m.createdBy());
        e.setModeratedBy(m.moderatedBy());
        e.setModerationReason(m.moderationReason());
        e.setCreatedAt(m.createdAt());
        e.setUpdatedAt(m.updatedAt());
        e.setModeratedAt(m.moderatedAt());
    }

    private List<VariantEntity> toVariantEntities(List<Product.VariantMemento> variants) {
        List<VariantEntity> result = new ArrayList<>();
        for (Product.VariantMemento vm : variants) {
            VariantEntity ve = new VariantEntity();
            ve.setVariantId(vm.variantId());
            ve.setName(vm.name());
            ve.getAttributes().putAll(vm.attributes());
            List<SkuEntity> skus = new ArrayList<>();
            for (Product.SkuMemento sm : vm.skus()) {
                SkuEntity se = new SkuEntity();
                se.setSkuId(sm.skuId());
                se.setSkuCode(sm.skuCode());
                se.setPriceAmount(sm.priceAmount());
                se.setCurrency(sm.currency());
                skus.add(se);
            }
            ve.setSkus(skus);
            result.add(ve);
        }
        return result;
    }

    private List<ProductImageEntity> toImageEntities(List<Product.ImageMemento> images) {
        List<ProductImageEntity> result = new ArrayList<>();
        for (Product.ImageMemento im : images) {
            ProductImageEntity ie = new ProductImageEntity();
            ie.setUrl(im.url());
            ie.setSortOrder(im.sortOrder());
            result.add(ie);
        }
        return result;
    }

    private Product toDomain(ProductEntity e) {
        List<Product.VariantMemento> variants = new ArrayList<>();
        for (VariantEntity ve : e.getVariants()) {
            List<Product.SkuMemento> skus = new ArrayList<>();
            for (SkuEntity se : ve.getSkus()) {
                skus.add(new Product.SkuMemento(se.getSkuId(), se.getSkuCode(), se.getPriceAmount(), se.getCurrency()));
            }
            variants.add(new Product.VariantMemento(ve.getVariantId(), ve.getName(), ve.getAttributes(), skus));
        }
        List<Product.ImageMemento> images = new ArrayList<>();
        for (ProductImageEntity ie : e.getImages()) {
            images.add(new Product.ImageMemento(ie.getUrl(), ie.getSortOrder()));
        }

        Product.Memento m = new Product.Memento(
                e.getId(), e.getProductId(), e.getMerchantId(), e.getName(), e.getDescription(),
                e.getStatus(), e.getBrandId(), e.getCategoryId(), e.isProductCreatedPublished(),
                e.getCreatedBy(), e.getModeratedBy(), e.getModerationReason(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getModeratedAt(), variants, images);
        return Product.fromMemento(m);
    }
}
