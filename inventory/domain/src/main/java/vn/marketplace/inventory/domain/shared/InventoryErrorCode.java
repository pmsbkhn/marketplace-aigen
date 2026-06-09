package vn.marketplace.inventory.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainErrorCode;

/**
 * Inventory-specific business error codes.
 *
 * <p>Each maps onto a coarse msfw {@link DomainErrorCode} (so the framework's GlobalExceptionHandler
 * picks the HTTP status) while carrying the precise business meaning the tech-spec/tests assert on.
 */
public enum InventoryErrorCode {
    INVALID_QUANTITY(DomainErrorCode.INVALID_ARGUMENT, "Quantity must be greater than zero"),
    NEGATIVE_STOCK(DomainErrorCode.INVALID_ARGUMENT, "Available stock cannot be negative"),
    INSUFFICIENT_STOCK(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Not enough available stock to reserve"),
    TENANT_MISMATCH(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Stock does not belong to the caller's tenant"),
    STOCK_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Stock not found for SKU"),
    RESERVATION_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Reservation not found");

    private final DomainErrorCode baseErrorCode;
    private final String defaultMessage;

    InventoryErrorCode(DomainErrorCode baseErrorCode, String defaultMessage) {
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
