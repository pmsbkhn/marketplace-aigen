package vn.marketplace.notification.adapter.delivery.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.marketplace.notification.application.delivery.AcceptNotification;
import vn.marketplace.notification.application.delivery.AcceptNotificationCmd;
import vn.marketplace.notification.application.delivery.DispatchNotification;
import vn.marketplace.notification.application.delivery.DispatchNotificationCmd;
import vn.marketplace.notification.application.delivery.GetNotification;
import vn.marketplace.notification.application.delivery.GetNotificationCmd;
import vn.marketplace.notification.application.delivery.NotificationIdView;
import vn.marketplace.notification.application.delivery.NotificationStatusView;

/** Transactional application boundary for the notification use-cases. */
@Service
public class NotificationFacade {

    private final AcceptNotification acceptNotification;
    private final DispatchNotification dispatchNotification;
    private final GetNotification getNotification;

    public NotificationFacade(AcceptNotification acceptNotification,
                              DispatchNotification dispatchNotification,
                              GetNotification getNotification) {
        this.acceptNotification = acceptNotification;
        this.dispatchNotification = dispatchNotification;
        this.getNotification = getNotification;
    }

    @Transactional
    public NotificationIdView accept(AcceptNotificationCmd cmd) {
        return acceptNotification.execute(cmd);
    }

    @Transactional
    public void dispatch(String notificationId) {
        dispatchNotification.execute(new DispatchNotificationCmd(notificationId));
    }

    /** Event stand-in convenience: accept then immediately dispatch (synchronous). */
    @Transactional
    public NotificationIdView acceptAndDispatch(AcceptNotificationCmd cmd) {
        NotificationIdView view = acceptNotification.execute(cmd);
        dispatchNotification.execute(new DispatchNotificationCmd(view.notificationId()));
        return view;
    }

    @Transactional(readOnly = true)
    public NotificationStatusView get(String notificationId, String callerId) {
        return getNotification.execute(new GetNotificationCmd(notificationId, callerId));
    }
}
