package vn.marketplace.notification.application.delivery;

/** Read model for a notification's delivery status. */
public record NotificationStatusView(String notificationId, String status, int attempts) {
}
