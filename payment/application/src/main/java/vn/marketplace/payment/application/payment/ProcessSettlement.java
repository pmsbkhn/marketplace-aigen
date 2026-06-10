package vn.marketplace.payment.application.payment;

/**
 * Use case: settle one completed order (consumes {@code OrderCompleted} from OMS) — release the
 * escrow hold (PAID guard), apply the commission policy, write the immutable WORM settlement
 * document and complete the settlement. Idempotent by {@code orderId}.
 */
public interface ProcessSettlement {

    SettlementView execute(ProcessSettlementCmd cmd);
}
