package vn.marketplace.payment.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainErrorCode;

/**
 * Payment-specific business error codes. Each maps onto a coarse msfw {@link DomainErrorCode} (so the
 * GlobalExceptionHandler picks the HTTP status) while carrying the precise business meaning the
 * tech-spec/tests assert on (AMOUNT_MISMATCH, PAYMENT_NOT_COMPLETED, MISSING_SETTLEMENT_DOCUMENT…).
 */
public enum PaymentErrorCode {
    AMOUNT_MISMATCH(DomainErrorCode.BUSINESS_RULE_VIOLATION,
            "Webhook amount does not match the escrow amount snapshot"),
    PAYMENT_NOT_COMPLETED(DomainErrorCode.BUSINESS_RULE_VIOLATION,
            "Escrow can only be released when the payment has been PAID"),
    MISSING_SETTLEMENT_DOCUMENT(DomainErrorCode.BUSINESS_RULE_VIOLATION,
            "A settlement cannot complete without its WORM document URI"),
    INVALID_TRANSITION(DomainErrorCode.BUSINESS_RULE_VIOLATION, "Illegal payment status transition"),
    INVALID_AMOUNT(DomainErrorCode.INVALID_ARGUMENT, "Amount must be greater than zero"),
    EMPTY_ALLOCATIONS(DomainErrorCode.INVALID_ARGUMENT, "An escrow must allocate to at least one order"),
    ALLOCATION_SUM_MISMATCH(DomainErrorCode.INVALID_ARGUMENT,
            "Escrow allocations must sum exactly to the total amount"),
    DUPLICATE_GATEWAY_TXN(DomainErrorCode.BUSINESS_RULE_VIOLATION,
            "This gateway transaction has already been processed"),
    ESCROW_HOLD_NOT_FOUND(DomainErrorCode.NOT_FOUND, "No escrow hold exists for this order"),
    PAYMENT_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Payment not found"),
    SETTLEMENT_NOT_FOUND(DomainErrorCode.NOT_FOUND, "Settlement not found"),
    BANK_ACCOUNT_REQUIRED(DomainErrorCode.INVALID_ARGUMENT,
            "A merchant bank account is required to create a payout");

    private final DomainErrorCode baseErrorCode;
    private final String defaultMessage;

    PaymentErrorCode(DomainErrorCode baseErrorCode, String defaultMessage) {
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
