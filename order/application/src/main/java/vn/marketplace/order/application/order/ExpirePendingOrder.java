package vn.marketplace.order.application.order;

/** Use-case port: cancel a PENDING order whose payment deadline passed (the delayed-timer handler). */
public interface ExpirePendingOrder {
    void execute(ExpirePendingOrderCmd cmd);
}
