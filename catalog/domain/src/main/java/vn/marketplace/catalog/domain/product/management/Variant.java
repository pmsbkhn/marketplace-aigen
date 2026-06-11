package vn.marketplace.catalog.domain.product.management;

import java.util.List;
import java.util.Map;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.catalog.domain.product.VariantId;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * A product variant (e.g. a color/size combination) owning one or more {@link Sku}s. Part of the
 * {@link Product} aggregate boundary.
 *
 * <p>Invariant: a variant must have at least one SKU (EMPTY_VARIANT).
 */
public class Variant implements Entity {

    /** Surrogate key threaded by the persistence adapter — {@code null} until first persisted. */
    private Long _id;
    private final VariantId id;
    private final String name;
    private final Map<String, String> attributes;
    private final List<Sku> skus;

    Variant(VariantId id, String name, Map<String, String> attributes, List<Sku> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new CatalogDomainException(CatalogErrorCode.EMPTY_VARIANT);
        }
        this.id = id;
        this.name = name;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.skus = List.copyOf(skus);
    }

    static Variant fromMemento(Product.VariantMemento m) {
        List<Sku> skus = m.skus().stream().map(Sku::fromMemento).toList();
        Variant variant = new Variant(new VariantId(m.variantId()), m.name(), m.attributes(), skus);
        variant.set_id(m._id());
        return variant;
    }

    public Long _id() {
        return _id;
    }

    public void set_id(Long _id) {
        this._id = _id;
    }

    public VariantId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public List<Sku> skus() {
        return skus;
    }
}
