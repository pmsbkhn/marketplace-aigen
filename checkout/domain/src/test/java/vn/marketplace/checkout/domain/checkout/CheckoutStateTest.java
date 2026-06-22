package vn.marketplace.checkout.domain.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.statemachine.IllegalStateTransitionException;
import tech.vsf.ptnt.msfw.domain.statemachine.StateMachine;

/** The checkout lifecycle FSM (msfw statemachine core) — the explicit source of truth for step order. */
class CheckoutStateTest {

    private StateMachine<CheckoutState, CheckoutTrigger> at(CheckoutState start) {
        return new StateMachine<>(start);
    }

    @Test
    void happyPath_walksTheLifecycleToRedirected() {
        StateMachine<CheckoutState, CheckoutTrigger> m = at(CheckoutState.PRICING);
        assertEquals(CheckoutState.RESERVING, m.fire(CheckoutTrigger.RESERVE));
        assertEquals(CheckoutState.ORDERING, m.fire(CheckoutTrigger.CREATE_ORDERS));
        assertEquals(CheckoutState.ESCROWING, m.fire(CheckoutTrigger.INIT_ESCROW));
        assertEquals(CheckoutState.REDIRECTED, m.fire(CheckoutTrigger.REDIRECT));
        assertTrue(m.isTerminal());
    }

    @Test
    void skippingAStepIsIllegal() {
        StateMachine<CheckoutState, CheckoutTrigger> m = at(CheckoutState.PRICING);
        assertThrows(IllegalStateTransitionException.class, () -> m.fire(CheckoutTrigger.INIT_ESCROW));
        assertEquals(CheckoutState.PRICING, m.current());
    }

    @Test
    void redeliveredReserveIsAnIdempotentNoOp() {
        StateMachine<CheckoutState, CheckoutTrigger> m = at(CheckoutState.PRICING);
        m.fire(CheckoutTrigger.RESERVE);
        assertEquals(CheckoutState.RESERVING, m.fire(CheckoutTrigger.RESERVE));
    }

    @Test
    void failIsReachableFromAnyNonTerminalStep_andIsTerminal() {
        assertEquals(CheckoutState.FAILED, at(CheckoutState.PRICING).fire(CheckoutTrigger.FAIL));
        assertEquals(CheckoutState.FAILED, at(CheckoutState.ESCROWING).fire(CheckoutTrigger.FAIL));
        assertTrue(CheckoutState.FAILED.isTerminal());
    }

    @Test
    void terminalIsFrozen() {
        assertThrows(IllegalStateTransitionException.class,
                () -> at(CheckoutState.REDIRECTED).fire(CheckoutTrigger.FAIL));
    }
}
