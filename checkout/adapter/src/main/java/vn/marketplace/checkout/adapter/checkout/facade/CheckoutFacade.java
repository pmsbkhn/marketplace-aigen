package vn.marketplace.checkout.adapter.checkout.facade;

import org.springframework.stereotype.Service;

import vn.marketplace.checkout.application.checkout.CheckoutResultView;
import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;
import vn.marketplace.checkout.application.checkout.GetCheckoutSession;
import vn.marketplace.checkout.application.checkout.SubmitCheckout;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd;

/**
 * Application boundary for the checkout use-cases. NOT {@code @Transactional}: Checkout has no
 * database — atomicity across contexts is the saga's compensation logic, not a local transaction.
 */
@Service
public class CheckoutFacade {

    private final SubmitCheckout submitCheckout;
    private final GetCheckoutSession getCheckoutSession;

    public CheckoutFacade(SubmitCheckout submitCheckout, GetCheckoutSession getCheckoutSession) {
        this.submitCheckout = submitCheckout;
        this.getCheckoutSession = getCheckoutSession;
    }

    public CheckoutResultView submit(SubmitCheckoutCmd cmd) {
        return submitCheckout.execute(cmd);
    }

    public CheckoutSession session(String idempotencyKey, String callerBuyerId) {
        return getCheckoutSession.execute(idempotencyKey, callerBuyerId);
    }
}
