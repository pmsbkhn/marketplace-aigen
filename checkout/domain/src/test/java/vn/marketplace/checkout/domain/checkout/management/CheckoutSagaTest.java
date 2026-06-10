package vn.marketplace.checkout.domain.checkout.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import vn.marketplace.checkout.domain.checkout.IdempotencyKey;
import vn.marketplace.checkout.domain.checkout.SagaState;
import vn.marketplace.checkout.domain.shared.InvalidTransitionException;

class CheckoutSagaTest {

    private CheckoutSaga newSaga() {
        return CheckoutSaga.start(new IdempotencyKey("IK-1"), "B-1");
    }

    @Test
    void skippingReservingIsRejected() { // TC-CHK-01: PRICING → ORDERING without RESERVING
        CheckoutSaga saga = newSaga();

        assertThrows(InvalidTransitionException.class, saga::markOrdering);
        assertEquals(SagaState.PRICING, saga.state(), "state untouched after the rejected jump");
    }

    @Test
    void happyPathProgressesLinearlyToRedirected() {
        CheckoutSaga saga = newSaga();

        saga.markReserving();
        saga.markOrdering();
        saga.markEscrowing(List.of("O-1", "O-2"));
        saga.complete("https://pg.example.com/pay/PAY-1");

        assertEquals(SagaState.REDIRECTED, saga.state());
        assertEquals(List.of("O-1", "O-2"), saga.orderIds());
        assertEquals("https://pg.example.com/pay/PAY-1", saga.paymentUrl());
    }

    @Test
    void terminalStatesAreFrozen() {
        CheckoutSaga done = newSaga();
        done.markReserving();
        done.markOrdering();
        done.markEscrowing(List.of("O-1"));
        done.complete("url");
        assertThrows(InvalidTransitionException.class, () -> done.fail("too late"));
        assertThrows(InvalidTransitionException.class, done::markReserving);

        CheckoutSaga failed = newSaga();
        failed.fail("escrow error");
        assertEquals(SagaState.FAILED, failed.state());
        assertEquals("escrow error", failed.failReason());
        assertThrows(InvalidTransitionException.class, failed::markReserving);
        assertThrows(InvalidTransitionException.class, () -> failed.fail("again"));
    }

    @Test
    void failIsReachableFromEveryNonTerminalStep() {
        CheckoutSaga atPricing = newSaga();
        atPricing.fail("catalog down");
        assertEquals(SagaState.FAILED, atPricing.state());

        CheckoutSaga atEscrowing = newSaga();
        atEscrowing.markReserving();
        atEscrowing.markOrdering();
        atEscrowing.markEscrowing(List.of("O-1"));
        atEscrowing.fail("escrow down");
        assertEquals(SagaState.FAILED, atEscrowing.state());
    }
}
