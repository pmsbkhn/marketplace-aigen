package vn.marketplace.notification.application.delivery;

/** Use-case port: ingest (durably accept) a notification request. */
public interface AcceptNotification {
    NotificationIdView execute(AcceptNotificationCmd cmd);
}
