package vn.marketplace.catalog.adapter.product.management.outbound.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import tech.vsf.ptnt.springcore.persistence.AbstractMementoJpaOa;
import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import tech.vsf.ptnt.springcore.persistence.SpringPages;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductEntity;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductImageEntity;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.SkuEntity;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.VariantEntity;
import vn.marketplace.catalog.application.product.ProductRepository;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.Sku;
import vn.marketplace.catalog.domain.product.management.Variant;

/**
 * Outbound persistence adapter: maps the {@link Product} aggregate ↔ JPA entities at the
 * {@link Product.Memento} level via msfw's {@code AbstractMementoJpaOa}, which owns the plumbing
 * (surrogate-key threading, identity lookups, {@code Criteria} → Specification translation, real
 * DB paging). {@code @Transactional} keeps the session open while the lazy variant/sku/image
 * collections are read during reconstruction.
 *
 * <p>Variant/SKU surrogate keys ride in the memento, so on update the children are merged in place
 * (no delete + reinsert, which would trip the unique {@code sku_code} index); images carry no
 * surrogate key and are rebuilt each save (orphanRemoval syncs the table). {@link #save} also
 * upserts by {@code productId}: a fresh aggregate ({@code _id == null}) for an existing productId
 * updates that row instead of violating the unique index, preserving the legacy adapter's semantics.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class ProductOa extends AbstractMementoJpaOa<Product, Product.Memento, ProductEntity>
        implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    protected JpaOaRepository<ProductEntity> jpa() {
        return productJpaRepository;
    }

    @Override
    public void save(Product aggregate) {
        if (aggregate._id() == null) {
            findEntity(aggregate.id()).ifPresent(existing -> {
                aggregate.set_id(existing.getId());
                threadChildIds(aggregate, existing);
            });
        }
        super.save(aggregate);
    }

    /**
     * A fresh aggregate carries no child surrogate keys; align them with the existing rows by
     * natural key (variantId / skuCode) so the merge updates in place instead of insert + orphan
     * delete — Hibernate flushes inserts first, which would trip the unique {@code sku_code} index.
     */
    private void threadChildIds(Product aggregate, ProductEntity existing) {
        Map<String, Long> variantIds = new HashMap<>();
        Map<String, Long> skuIds = new HashMap<>();
        for (VariantEntity ve : existing.getVariants()) {
            variantIds.put(ve.getVariantId(), ve.getId());
            for (SkuEntity se : ve.getSkus()) {
                skuIds.put(se.getSkuCode(), se.getId());
            }
        }
        for (Variant v : aggregate.variants()) {
            if (v._id() == null) {
                v.set_id(variantIds.get(v.id().value()));
            }
            for (Sku s : v.skus()) {
                if (s._id() == null) {
                    s.set_id(skuIds.get(s.code().value()));
                }
            }
        }
    }

    /** Joins the variants/skus collections — beyond the Criteria translator, so a derived query. */
    @Override
    public List<Product> findBySkuCodes(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }
        return productJpaRepository.findBySkuCodes(skuCodes).stream()
                .map(this::reconstitute)
                .toList();
    }

    /** Price bounds match against the skus collection — beyond the translator, so a hand-written query. */
    @Override
    public PagedSearchResult<Product> searchActive(String text,
                                                   String categoryId,
                                                   String brandId,
                                                   Long priceMin,
                                                   Long priceMax,
                                                   Pagination pagination) {
        String pattern = text == null || text.isBlank()
                ? null
                : "%" + text.toLowerCase(Locale.ROOT) + "%";
        Page<ProductEntity> page = productJpaRepository.searchActive(
                pattern, categoryId, brandId, priceMin, priceMax, SpringPages.toPageable(pagination));
        return SpringPages.toPagedSearchResult(page.map(this::reconstitute));
    }

    // ---- memento ↔ entity mapping (mechanical; root surrogate id is threaded by the base class) ----

    @Override
    protected ProductEntity fromMemento(Product.Memento m) {
        ProductEntity e = new ProductEntity();
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

        for (Product.VariantMemento vm : m.variants()) {
            VariantEntity ve = new VariantEntity();
            ve.setId(vm._id());
            ve.setVariantId(vm.variantId());
            ve.setName(vm.name());
            ve.getAttributes().putAll(vm.attributes());
            for (Product.SkuMemento sm : vm.skus()) {
                SkuEntity se = new SkuEntity();
                se.setId(sm._id());
                se.setSkuId(sm.skuId());
                se.setSkuCode(sm.skuCode());
                se.setPriceAmount(sm.priceAmount());
                se.setCurrency(sm.currency());
                ve.getSkus().add(se);
            }
            e.getVariants().add(ve);
        }
        for (Product.ImageMemento im : m.images()) {
            ProductImageEntity ie = new ProductImageEntity();
            ie.setUrl(im.url());
            ie.setSortOrder(im.sortOrder());
            e.getImages().add(ie);
        }
        return e;
    }

    @Override
    protected Product.Memento toMemento(ProductEntity e) {
        List<Product.VariantMemento> variants = new ArrayList<>();
        for (VariantEntity ve : e.getVariants()) {
            List<Product.SkuMemento> skus = new ArrayList<>();
            for (SkuEntity se : ve.getSkus()) {
                skus.add(new Product.SkuMemento(se.getId(), se.getSkuId(), se.getSkuCode(),
                        se.getPriceAmount(), se.getCurrency()));
            }
            variants.add(new Product.VariantMemento(ve.getId(), ve.getVariantId(), ve.getName(),
                    new HashMap<>(ve.getAttributes()), skus));
        }
        List<Product.ImageMemento> images = new ArrayList<>();
        for (ProductImageEntity ie : e.getImages()) {
            images.add(new Product.ImageMemento(ie.getUrl(), ie.getSortOrder()));
        }
        return new Product.Memento(
                e.getId(), e.getProductId(), e.getMerchantId(), e.getName(), e.getDescription(),
                e.getStatus(), e.getBrandId(), e.getCategoryId(), e.isProductCreatedPublished(),
                e.getCreatedBy(), e.getModeratedBy(), e.getModerationReason(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getModeratedAt(), variants, images);
    }

    @Override
    protected Product restore(Product.Memento memento) {
        return Product.restore(memento);
    }

    @Override
    protected Criteria identityCriteria(Identity<?> id) {
        return Criteria.where("productId").eq(id.value());
    }
}
