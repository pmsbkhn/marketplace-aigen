package vn.marketplace.checkout.domain.checkout.management;

import java.time.LocalDateTime;
import java.util.List;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.IdempotencyKey;
import vn.marketplace.checkout.domain.checkout.SagaState;
import vn.marketplace.checkout.domain.shared.InvalidTransitionException;

/**
 * CheckoutSaga aggregate root — the in-memory orchestration state of one checkout request.
 * Checkout is stateless: the saga lives for the duration of the request; only its projection
 * (state, orderIds, paymentUrl) is cached in the session store for idempotency.
 *
 * <p>Enforced invariants (tech-spec §2):
 * <ol>
 *   <li><b>Linear one-way progression</b> {@code PRICING → RESERVING → ORDERING → ESCROWING →
 *       REDIRECTED}; skipping a step throws {@link InvalidTransitionException} (TC-CHK-01).</li>
 *   <li><b>Terminal immutability</b> — REDIRECTED/FAILED are frozen forever.</li>
 *   <li>{@code fail(reason)} is reachable from any non-terminal state (a saga can break at any
 *       step), and records the reason for the cached failure result.</li>
 * </ol>
 * The reverse-order compensation rule (cancel orders BEFORE releasing stock, TC-CHK-02) is executed
 * by the use case — the saga only guarantees the state bookkeeping around it.
 */
public class CheckoutSaga extends Aggregate<IdempotencyKey> {

    private final String buyerId;
    private SagaState state;
    private List<String> orderIds = List.of();
    private String paymentUrl;
    private String failReason;
    private final LocalDateTime startedAt;
    private LocalDateTime updatedAt;

    private CheckoutSaga(IdempotencyKey key, String buyerId) {
        this.id = key;
        this.buyerId = buyerId;
        this.state = SagaState.PRICING;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = this.startedAt;
    }

    /** Starts a new saga at PRICING (the first thing a checkout does is fetch authoritative prices). */
    public static CheckoutSaga start(IdempotencyKey key, String buyerId) {
        return new CheckoutSaga(key, buyerId);
    }

    // ---- verb methods (the only way to advance; one step at a time) ----

    /** PRICING → RESERVING (prices snapshotted, about to hold stock). */
    public void markReserving() {
        advance(SagaState.PRICING, SagaState.RESERVING, "RESERVE_STOCK");
    }

    /** RESERVING → ORDERING (stock held, about to create the per-merchant pending orders). */
    public void markOrdering() {
        advance(SagaState.RESERVING, SagaState.ORDERING, "CREATE_ORDERS");
    }

    /** ORDERING → ESCROWING (orders created, about to init the whole-cart escrow). */
    public void markEscrowing(List<String> createdOrderIds) {
        advance(SagaState.ORDERING, SagaState.ESCROWING, "INIT_ESCROW");
        this.orderIds = List.copyOf(createdOrderIds);
    }

    /** ESCROWING → REDIRECTED (terminal happy end — buyer goes to the payment gateway). */
    public void complete(String paymentUrl) {
        advance(SagaState.ESCROWING, SagaState.REDIRECTED, "REDIRECT");
        this.paymentUrl = paymentUrl;
    }

    /** Any non-terminal → FAILED (terminal); records the reason for the cached failure. */
    public void fail(String reason) {
        if (state.isTerminal()) {
            throw new InvalidTransitionException(state.name(), "FAIL");
        }
        this.state = SagaState.FAILED;
        this.failReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    private void advance(SagaState expected, SagaState next, String event) {
        if (this.state != expected) {
            throw new InvalidTransitionException(this.state.name(), event);
        }
        this.state = next;
        this.updatedAt = LocalDateTime.now();
    }

    // ---- queries / accessors ----

    public String buyerId() {
        return buyerId;
    }

    public SagaState state() {
        return state;
    }

    public List<String> orderIds() {
        return orderIds;
    }

    public String paymentUrl() {
        return paymentUrl;
    }

    public String failReason() {
        return failReason;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }
}
