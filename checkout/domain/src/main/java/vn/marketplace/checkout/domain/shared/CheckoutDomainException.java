package vn.marketplace.checkout.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainException;

/**
 * Domain exception for the Checkout context, parameterised by a {@link CheckoutErrorCode}. Extends
 * msfw's abstract {@link DomainException} so the GlobalExceptionHandler maps it to the right HTTP
 * status; the precise business reason is carried by {@link #checkoutErrorCode()}.
 */
public class CheckoutDomainException extends DomainException {

    private final CheckoutErrorCode checkoutErrorCode;

    public CheckoutDomainException(CheckoutErrorCode code) {
        super(code.baseErrorCode(), code.defaultMessage());
        this.checkoutErrorCode = code;
    }

    public CheckoutDomainException(CheckoutErrorCode code, String message) {
        super(code.baseErrorCode(), message);
        this.checkoutErrorCode = code;
    }

    public CheckoutErrorCode checkoutErrorCode() {
        return checkoutErrorCode;
    }
}
