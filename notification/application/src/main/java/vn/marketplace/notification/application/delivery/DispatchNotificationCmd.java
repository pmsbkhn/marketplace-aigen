package vn.marketplace.notification.application.delivery;

/** Dispatch command — render and attempt delivery of one accepted notification. */
public record DispatchNotificationCmd(String notificationId) {
}
