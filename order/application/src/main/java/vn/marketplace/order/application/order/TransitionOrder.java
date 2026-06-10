package vn.marketplace.order.application.order;

/** Use-case port: drive a forward order transition (payment received / ship / confirm-delivery). */
public interface TransitionOrder {
    void execute(TransitionOrderCmd cmd);
}
