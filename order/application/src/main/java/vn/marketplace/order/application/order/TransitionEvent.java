package vn.marketplace.order.application.order;

/** The events that drive a forward order transition (cancellation goes through {@code CancelOrder}). */
public enum TransitionEvent {
    PAYMENT_RECEIVED, // PENDING → TO_SHIP
    MERCHANT_SHIP,    // TO_SHIP → SHIPPED
    BUYER_CONFIRM,    // SHIPPED → COMPLETED
    AUTO_COMPLETE     // SHIPPED → COMPLETED (auto)
}
