package vn.marketplace.payment.application.payment;

/** Use case: read one payment by its checkout {@code orderRef} (status polling / reconciliation). */
public interface GetPayment {

    PaymentView execute(GetPaymentCmd cmd);
}
