package vn.marketplace.notification.domain.delivery;

/** Delivery channel. {@code AUTO} means fall back sequentially across the user's preferred channels. */
public enum Channel {
    EMAIL,
    SMS,
    PUSH,
    AUTO
}
