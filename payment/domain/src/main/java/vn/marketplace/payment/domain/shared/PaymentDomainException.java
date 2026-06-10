package vn.marketplace.payment.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainException;

/**
 * Domain exception for the Payment context, parameterised by a {@link PaymentErrorCode}. Extends msfw's
 * abstract {@link DomainException} so the GlobalExceptionHandler maps it to the right HTTP status; the
 * precise business reason is carried by {@link #paymentErrorCode()}.
 */
public class PaymentDomainException extends DomainException {

    private final PaymentErrorCode paymentErrorCode;

    public PaymentDomainException(PaymentErrorCode code) {
        super(code.baseErrorCode(), code.defaultMessage());
        this.paymentErrorCode = code;
    }

    public PaymentDomainException(PaymentErrorCode code, String message) {
        super(code.baseErrorCode(), message);
        this.paymentErrorCode = code;
    }

    public PaymentErrorCode paymentErrorCode() {
        return paymentErrorCode;
    }
}
