package vn.marketplace.checkout.domain.checkout;

import tech.vsf.ptnt.msfw.domain.statemachine.IllegalStateTransitionException;
import tech.vsf.ptnt.msfw.domain.statemachine.State;

/**
 * The checkout orchestration lifecycle as an explicit, self-deciding finite state machine, built on
 * the msfw {@code statemachine} core. Linear:
 * {@code PRICING → RESERVING → ORDERING → ESCROWING → REDIRECTED}, with {@code FAILED} reachable
 * from any non-terminal step; {@code REDIRECTED}/{@code FAILED} are terminal.
 *
 * <p>This is the single source of truth for the legal step order. {@code SubmitCheckoutUc} drives it
 * <b>synchronously</b> (firing a {@link CheckoutTrigger} as it enters each {@code CompensatingWorkflow}
 * step); the same graph is ready to be driven asynchronously by a {@code StateMachineSaga} if checkout
 * ever waits on events. A redelivered {@code RESERVE} while already {@code RESERVING} is an idempotent
 * no-op (returns {@code this}); an out-of-order trigger throws {@link IllegalStateTransitionException}.
 */
public enum CheckoutState implements State<CheckoutState, CheckoutTrigger> {
    PRICING {
        @Override
        public CheckoutState on(CheckoutTrigger t) {
            return switch (t) {
                case RESERVE -> RESERVING;
                case FAIL -> FAILED;
                default -> throw IllegalStateTransitionException.of(this, t);
            };
        }
    },
    RESERVING {
        @Override
        public CheckoutState on(CheckoutTrigger t) {
            return switch (t) {
                case RESERVE -> this;                 // idempotent: redelivered reserve is ignored
                case CREATE_ORDERS -> ORDERING;
                case FAIL -> FAILED;
                default -> throw IllegalStateTransitionException.of(this, t);
            };
        }
    },
    ORDERING {
        @Override
        public CheckoutState on(CheckoutTrigger t) {
            return switch (t) {
                case INIT_ESCROW -> ESCROWING;
                case FAIL -> FAILED;
                default -> throw IllegalStateTransitionException.of(this, t);
            };
        }
    },
    ESCROWING {
        @Override
        public CheckoutState on(CheckoutTrigger t) {
            return switch (t) {
                case REDIRECT -> REDIRECTED;
                case FAIL -> FAILED;
                default -> throw IllegalStateTransitionException.of(this, t);
            };
        }
    },
    REDIRECTED {
        @Override
        public CheckoutState on(CheckoutTrigger t) {
            throw IllegalStateTransitionException.of(this, t);
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    },
    FAILED {
        @Override
        public CheckoutState on(CheckoutTrigger t) {
            throw IllegalStateTransitionException.of(this, t);
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    }
}
