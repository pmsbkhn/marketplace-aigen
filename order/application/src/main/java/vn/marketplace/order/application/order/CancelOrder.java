package vn.marketplace.order.application.order;

/** Use-case port: cancel a PENDING/TO_SHIP order (saga compensation, payment failure, buyer/admin). */
public interface CancelOrder {
    void execute(CancelOrderCmd cmd);
}
