package vn.marketplace.checkout.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainErrorCode;

/**
 * Checkout-specific business error codes. Each maps onto a coarse msfw {@link DomainErrorCode} (so
 * the GlobalExceptionHandler picks the HTTP status) while carrying the precise business meaning the
 * tech-spec/tests assert on.
 */
public enum CheckoutErrorCode {
    EMPTY_CART(DomainErrorCode.INVALID_ARGUMENT, "A checkout must contain at least one item"),
    INVALID_QUANTITY(DomainErrorCode.INVALID_ARGUMENT, "Item quantity must be greater than zero"),
    INVALID_PRICE(DomainErrorCode.INVALID_ARGUMENT, "Catalog returned a non-positive price"),
    TOO_MANY_ITEMS(DomainErrorCode.INVALID_ARGUMENT, "Cart exceeds the maximum number of items"),
    TOO_MANY_MERCHANTS(DomainErrorCode.INVALID_ARGUMENT, "Cart exceeds the maximum number of merchants"),
    SKU_UNAVAILABLE(DomainErrorCode.BUSINESS_RULE_VIOLATION,
            "One or more SKUs do not exist or are not ACTIVE"),
    OUT_OF_STOCK(DomainErrorCode.BUSINESS_RULE_VIOLATION, "One or more SKUs are out of stock"),
    CHECKOUT_IN_PROGRESS(DomainErrorCode.DUPLICATE_RESOURCE,
            "A checkout with this idempotency key is already being processed"),
    CHECKOUT_FAILED(DomainErrorCode.INTERNAL_ERROR,
            "Checkout failed after compensation — safe to retry"),
    INVALID_TRANSITION(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Illegal saga state transition"),
    SESSION_NOT_FOUND(DomainErrorCode.NOT_FOUND, "No checkout session for this key"),
    TENANT_MISMATCH(DomainErrorCode.BUSINESS_RULE_VIOLATION,
            "Checkout session does not belong to the caller");

    private final DomainErrorCode baseErrorCode;
    private final String defaultMessage;

    CheckoutErrorCode(DomainErrorCode baseErrorCode, String defaultMessage) {
        this.baseErrorCode = baseErrorCode;
        this.defaultMessage = defaultMessage;
    }

    public DomainErrorCode baseErrorCode() {
        return baseErrorCode;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
