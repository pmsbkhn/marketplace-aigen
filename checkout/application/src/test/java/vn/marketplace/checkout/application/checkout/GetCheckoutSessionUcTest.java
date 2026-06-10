package vn.marketplace.checkout.application.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

class GetCheckoutSessionUcTest {

    private CheckoutTestFakes fakes;
    private GetCheckoutSessionUc uc;

    @BeforeEach
    void setUp() {
        fakes = new CheckoutTestFakes();
        uc = new GetCheckoutSessionUc(fakes.sessions);
        fakes.sessions.saveCompleted("IK-1", "B-1", List.of("O-1"), "url", 100);
    }

    @Test
    void ownerReadsOwnSession() {
        CheckoutSession session = uc.execute("IK-1", "B-1");
        assertEquals(CheckoutSession.STATE_REDIRECTED, session.state());
    }

    @Test
    void foreignCallerGetsNotFound_antiIdor() {
        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute("IK-1", "B-2"));
        assertEquals(CheckoutErrorCode.SESSION_NOT_FOUND, ex.checkoutErrorCode());
    }

    @Test
    void missingSessionIsNotFound() {
        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute("IK-404", "B-1"));
        assertEquals(CheckoutErrorCode.SESSION_NOT_FOUND, ex.checkoutErrorCode());
    }
}
