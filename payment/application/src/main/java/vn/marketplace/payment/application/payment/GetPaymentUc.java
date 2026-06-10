package vn.marketplace.payment.application.payment;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/** Read-only lookup — no state change, no {@code @EventPublishHandler}. */
public class GetPaymentUc implements GetPayment {

    private final Repository<Payment> paymentRepository;

    public GetPaymentUc(Repository<Payment> paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentView execute(GetPaymentCmd cmd) {
        return paymentRepository.findBy(PaymentCriteria.byOrderRef(cmd.orderRef())).stream()
                .findFirst()
                .map(PaymentView::from)
                .orElseThrow(() -> new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "No payment for orderRef " + cmd.orderRef()));
    }
}
