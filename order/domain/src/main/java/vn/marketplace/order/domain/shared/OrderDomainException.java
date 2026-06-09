package vn.marketplace.order.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainException;

/**
 * Domain exception for the Order context, parameterised by an {@link OrderErrorCode}. Extends msfw's
 * abstract {@link DomainException} so the GlobalExceptionHandler maps it to the right HTTP status; the
 * precise business reason is carried by {@link #orderErrorCode()}.
 */
public class OrderDomainException extends DomainException {

    private final OrderErrorCode orderErrorCode;

    public OrderDomainException(OrderErrorCode code) {
        super(code.baseErrorCode(), code.defaultMessage());
        this.orderErrorCode = code;
    }

    public OrderDomainException(OrderErrorCode code, String message) {
        super(code.baseErrorCode(), message);
        this.orderErrorCode = code;
    }

    public OrderErrorCode orderErrorCode() {
        return orderErrorCode;
    }
}
