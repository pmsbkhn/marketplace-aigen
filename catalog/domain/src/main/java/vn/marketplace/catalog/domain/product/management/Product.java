package vn.marketplace.catalog.domain.product.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.core.Snapshotable;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import vn.marketplace.catalog.domain.product.BrandId;
import vn.marketplace.catalog.domain.product.CategoryId;
import vn.marketplace.catalog.domain.product.MerchantId;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.ProductImage;
import vn.marketplace.catalog.domain.product.ProductStatus;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * Catalog aggregate root — the product source of truth (Product → Variant → SKU).
 *
 * <p>Enforced invariants:
 * <ul>
 *   <li>New products are always {@link ProductStatus#PENDING}.</li>
 *   <li>Status machine: PENDING → (ACTIVE | REJECTED); ACTIVE → DEACTIVATED; REJECTED → PENDING.</li>
 *   <li>Moderation gate: only an Admin {@link Actor} may approve/reject (else UNAUTHORIZED_ACTION).</li>
 *   <li>A product must have ≥1 variant, each variant ≥1 SKU; SKU price &gt; 0.</li>
 *   <li>{@code merchantId} is set at creation and never changes (tenant-immutable — no mutator exists).</li>
 *   <li>{@code ProductCreated} is published exactly once, when the product first becomes ACTIVE.</li>
 * </ul>
 * State is only ever changed through the verb methods below — there are no public setters.
 */
public class Product extends Aggregate<ProductId> implements Snapshotable<Product.Memento> {

    private final MerchantId merchantId;
    private String name;
    private String description;
    private ProductStatus status;
    private BrandId brandId;
    private CategoryId categoryId;
    private final List<Variant> variants;
    private final List<ProductImage> images;
    private boolean productCreatedPublished;
    private final String createdBy;
    private String moderatedBy;
    private String moderationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime moderatedAt;

    /** New product: always PENDING. Package-private — built via {@link ProductFactory}. */
    Product(ProductId id,
            MerchantId merchantId,
            String name,
            String description,
            BrandId brandId,
            CategoryId categoryId,
            List<Variant> variants,
            List<ProductImage> images,
            String createdBy) {
        if (variants == null || variants.isEmpty()) {
            throw new CatalogDomainException(CatalogErrorCode.EMPTY_PRODUCT);
        }
        this.id = id;
        this.merchantId = merchantId;
        this.name = name;
        this.description = description;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.variants = new ArrayList<>(variants);
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        this.status = ProductStatus.PENDING;
        this.productCreatedPublished = false;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Reconstruction constructor — restores full state, raises no events. */
    private Product(Memento m, List<Variant> variants, List<ProductImage> images) {
        this.id = new ProductId(m.productId());
        this.set_id(m._id());
        this.merchantId = new MerchantId(m.merchantId());
        this.name = m.name();
        this.description = m.description();
        this.status = ProductStatus.valueOf(m.status());
        this.brandId = m.brandId() == null ? null : new BrandId(m.brandId());
        this.categoryId = m.categoryId() == null ? null : new CategoryId(m.categoryId());
        this.variants = new ArrayList<>(variants);
        this.images = new ArrayList<>(images);
        this.productCreatedPublished = m.productCreatedPublished();
        this.createdBy = m.createdBy();
        this.moderatedBy = m.moderatedBy();
        this.moderationReason = m.moderationReason();
        this.createdAt = m.createdAt();
        this.updatedAt = m.updatedAt();
        this.moderatedAt = m.moderatedAt();
    }

    // ---- verb methods (the only way to change state) ----

    /** Admin approves a PENDING product → ACTIVE. Publishes {@code ProductCreated} on first activation. */
    public void approve(Actor moderator) {
        requireAdmin(moderator);
        if (this.status != ProductStatus.PENDING) {
            throw new CatalogDomainException(CatalogErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = ProductStatus.ACTIVE;
        this.moderatedBy = moderator.id();
        this.moderatedAt = LocalDateTime.now();
        this.updatedAt = this.moderatedAt;
        if (!this.productCreatedPublished) {
            DomainEventPublisher.publish(new ProductCreated(this, DTime.now()));
            this.productCreatedPublished = true;
        }
    }

    /** Admin rejects a PENDING product → REJECTED (reason required). */
    public void reject(Actor moderator, String reason) {
        requireAdmin(moderator);
        if (this.status != ProductStatus.PENDING) {
            throw new CatalogDomainException(CatalogErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (reason == null || reason.isBlank()) {
            throw new CatalogDomainException(CatalogErrorCode.REJECTION_REASON_REQUIRED);
        }
        this.status = ProductStatus.REJECTED;
        this.moderatedBy = moderator.id();
        this.moderationReason = reason;
        this.moderatedAt = LocalDateTime.now();
        this.updatedAt = this.moderatedAt;
    }

    /** Merchant resubmits a REJECTED product for moderation → PENDING. */
    public void resubmit() {
        if (this.status != ProductStatus.REJECTED) {
            throw new CatalogDomainException(CatalogErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = ProductStatus.PENDING;
        this.moderationReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** Take an ACTIVE product off the storefront → DEACTIVATED (terminal). */
    public void deactivate() {
        if (this.status != ProductStatus.ACTIVE) {
            throw new CatalogDomainException(CatalogErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = ProductStatus.DEACTIVATED;
        this.updatedAt = LocalDateTime.now();
    }

    /** Merchant changes the price of one of this product's SKUs (price &gt; 0). */
    public void changeSkuPrice(SkuCode code, long newAmount) {
        Sku sku = findSku(code).orElseThrow(
                () -> new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND, "SKU not found: " + code));
        sku.changePrice(newAmount);
        this.updatedAt = LocalDateTime.now();
    }

    private static void requireAdmin(Actor moderator) {
        if (moderator == null || !moderator.isAdmin()) {
            throw new CatalogDomainException(CatalogErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    // ---- queries / accessors ----

    public Optional<Sku> findSku(SkuCode code) {
        return variants.stream()
                .flatMap(v -> v.skus().stream())
                .filter(s -> s.code().equals(code))
                .findFirst();
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public ProductStatus status() {
        return status;
    }

    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }

    public BrandId brandId() {
        return brandId;
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public List<Variant> variants() {
        return List.copyOf(variants);
    }

    public List<ProductImage> images() {
        return List.copyOf(images);
    }

    public boolean productCreatedPublished() {
        return productCreatedPublished;
    }

    public String createdBy() {
        return createdBy;
    }

    public String moderatedBy() {
        return moderatedBy;
    }

    public String moderationReason() {
        return moderationReason;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public LocalDateTime moderatedAt() {
        return moderatedAt;
    }

    // ---- Memento (persistence reconstruction) ----

    public static Product restore(Memento m) {
        List<Variant> variants = m.variants().stream().map(Variant::fromMemento).toList();
        List<ProductImage> images = m.images().stream()
                .map(im -> new ProductImage(im.url(), im.sortOrder()))
                .toList();
        return new Product(m, variants, images);
    }

    @Override
    public Memento snapshot() {
        List<VariantMemento> variantMementos = new ArrayList<>();
        for (Variant v : variants) {
            List<SkuMemento> skuMementos = new ArrayList<>();
            for (Sku s : v.skus()) {
                skuMementos.add(new SkuMemento(s._id(), s._version(), s.id().value(), s.code().value(),
                        s.price().amount(), s.price().currency().name()));
            }
            variantMementos.add(new VariantMemento(v._id(), v._version(), v.id().value(), v.name(),
                    v.attributes(), skuMementos));
        }
        List<ImageMemento> imageMementos = images.stream()
                .map(im -> new ImageMemento(im.url(), im.sortOrder()))
                .toList();
        return new Memento(_id(), id.value(), merchantId.value(), name, description, status.name(),
                brandId == null ? null : brandId.value(),
                categoryId == null ? null : categoryId.value(),
                productCreatedPublished, createdBy, moderatedBy, moderationReason,
                createdAt, updatedAt, moderatedAt, variantMementos, imageMementos);
    }

    public record Memento(Long _id,
                          String productId,
                          String merchantId,
                          String name,
                          String description,
                          String status,
                          String brandId,
                          String categoryId,
                          boolean productCreatedPublished,
                          String createdBy,
                          String moderatedBy,
                          String moderationReason,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          LocalDateTime moderatedAt,
                          List<VariantMemento> variants,
                          List<ImageMemento> images) {
    }

    public record VariantMemento(Long _id,
                                 Long _version,
                                 String variantId,
                                 String name,
                                 Map<String, String> attributes,
                                 List<SkuMemento> skus) {
    }

    public record SkuMemento(Long _id, Long _version, String skuId, String skuCode,
                             long priceAmount, String currency) {
    }

    public record ImageMemento(String url, int sortOrder) {
    }
}
