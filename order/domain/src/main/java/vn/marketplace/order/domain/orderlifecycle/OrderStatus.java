package vn.marketplace.order.domain.orderlifecycle;

/**
 * Order lifecycle states. Transitions are one-directional through the state machine enforced by the
 * {@code Order} aggregate: {@code PENDING → TO_SHIP → SHIPPED → COMPLETED}, with {@code CANCELLED}
 * reachable from PENDING/TO_SHIP. {@code COMPLETED} and {@code CANCELLED} are terminal.
 */
public enum OrderStatus {
    PENDING,
    TO_SHIP,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
