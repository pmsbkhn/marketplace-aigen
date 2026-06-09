package vn.marketplace.order.application.order;

/** Use-case port: create a PENDING order (called by Checkout, one order per merchant). */
public interface CreatePendingOrder {
    OrderIdView execute(CreatePendingOrderCmd cmd);
}
