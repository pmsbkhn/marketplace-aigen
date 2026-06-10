package vn.marketplace.notification.application.delivery;

/** Use-case port: render + send an accepted notification (the dispatcher worker). */
public interface DispatchNotification {
    void execute(DispatchNotificationCmd cmd);
}
