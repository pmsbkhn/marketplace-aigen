package vn.marketplace.order.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainErrorCode;

/**
 * Order-specific business error codes. Each maps onto a coarse msfw {@link DomainErrorCode} (so the
 * GlobalExceptionHandler picks the HTTP status) while carrying the precise business meaning the
 * tech-spec/tests assert on.
 */
public enum OrderErrorCode {
    EMPTY_CART(DomainErrorCode.INVALID_ARGUMENT, "An order must contain at least one item"),
    INVALID_QUANTITY(DomainErrorCode.INVALID_ARGUMENT, "Item quantity must be greater than zero"),
    INVALID_PRICE(DomainErrorCode.INVALID_ARGUMENT, "Item price must be greater than zero"),
    TRACKING_REQUIRED(DomainErrorCode.INVALID_ARGUMENT, "A tracking number is required to ship"),
    INVALID_TRANSITION(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Illegal order status transition"),
    TENANT_MISMATCH(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Order does not belong to the caller's tenant"),
    ORDER_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Order not found");

    private final DomainErrorCode baseErrorCode;
    private final String defaultMessage;

    OrderErrorCode(DomainErrorCode baseErrorCode, String defaultMessage) {
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
