package vn.marketplace.notification.application.delivery;

/** Use-case port: read a notification's current delivery status. Read-only. */
public interface GetNotification {
    NotificationStatusView execute(GetNotificationCmd cmd);
}
