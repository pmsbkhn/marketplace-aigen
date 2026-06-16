package vn.marketplace.inventory.domain.stock;

import tech.vsf.ptnt.msfw.domain.core.StringIdentity;

/** Owning merchant of a {@link vn.marketplace.inventory.domain.stock.management.Stock} (tenant scope). */
public class MerchantId extends StringIdentity {

    public MerchantId(String value) {
        super(value);
    }
}
