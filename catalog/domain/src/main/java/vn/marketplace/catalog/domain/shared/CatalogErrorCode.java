package vn.marketplace.catalog.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainErrorCode;

/**
 * Catalog-specific business error codes.
 *
 * <p>msfw's {@link DomainErrorCode} is a closed enum of coarse codes (used by the framework's
 * GlobalExceptionHandler to pick an HTTP status). These fine-grained catalog codes carry the
 * precise business meaning the tech-spec/tests assert on, while still mapping onto a base
 * {@link DomainErrorCode} so HTTP mapping keeps working.
 */
public enum CatalogErrorCode {
    INVALID_PRICE(DomainErrorCode.INVALID_ARGUMENT, "SKU price must be greater than zero"),
    EMPTY_PRODUCT(DomainErrorCode.INVALID_ARGUMENT, "Product must have at least one variant"),
    EMPTY_VARIANT(DomainErrorCode.INVALID_ARGUMENT, "Variant must have at least one SKU"),
    REJECTION_REASON_REQUIRED(DomainErrorCode.INVALID_ARGUMENT, "A reason is required when rejecting a product"),
    UNAUTHORIZED_ACTION(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Actor is not authorized to perform this action"),
    INVALID_STATUS_TRANSITION(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Illegal product status transition"),
    BRAND_NOT_APPROVED(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Referenced brand is not approved"),
    TENANT_MISMATCH(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Resource does not belong to the caller's tenant"),
    PRODUCT_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Product not found"),
    DUPLICATE_SKU(DomainErrorCode.DUPLICATE_RESOURCE, "SKU code already exists");

    private final DomainErrorCode baseErrorCode;
    private final String defaultMessage;

    CatalogErrorCode(DomainErrorCode baseErrorCode, String defaultMessage) {
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
