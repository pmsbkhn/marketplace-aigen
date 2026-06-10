package vn.marketplace.notification.application.delivery;

/** Request a notification's status. {@code callerId} must match the original sender (IDOR guard). */
public record GetNotificationCmd(String notificationId, String callerId) {
}
