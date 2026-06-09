package vn.marketplace.catalog.domain.product.management;

import tech.vsf.ptnt.msfw.domain.core.Entity;
import vn.marketplace.catalog.domain.product.Currency;
import vn.marketplace.catalog.domain.product.Money;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.SkuId;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * A stock-keeping unit inside a {@link Variant}. Holds the unique {@link SkuCode} and the
 * source-of-truth price. Part of the {@link Product} aggregate boundary — only created/mutated
 * through the aggregate, never independently.
 *
 * <p>Invariant: price &gt; 0 (INVALID_PRICE) — enforced on creation and on every price change.
 */
public class Sku implements Entity {

    private final SkuId id;
    private final SkuCode code;
    private Money price;

    Sku(SkuId id, SkuCode code, long priceAmount, Currency currency) {
        if (priceAmount <= 0) {
            throw new CatalogDomainException(CatalogErrorCode.INVALID_PRICE);
        }
        this.id = id;
        this.code = code;
        this.price = Money.of(priceAmount, currency);
    }

    /** Reconstruction from persistence — no validation/events. */
    static Sku fromMemento(Product.SkuMemento m) {
        Sku sku = new Sku(new SkuId(m.skuId()), new SkuCode(m.skuCode()),
                m.priceAmount(), Currency.of(m.currency()));
        return sku;
    }

    void changePrice(long newAmount) {
        if (newAmount <= 0) {
            throw new CatalogDomainException(CatalogErrorCode.INVALID_PRICE);
        }
        this.price = Money.of(newAmount, this.price.currency());
    }

    public SkuId id() {
        return id;
    }

    public SkuCode code() {
        return code;
    }

    public Money price() {
        return price;
    }
}
