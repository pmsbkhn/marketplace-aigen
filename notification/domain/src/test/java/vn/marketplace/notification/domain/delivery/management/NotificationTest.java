package vn.marketplace.notification.domain.delivery.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import vn.marketplace.notification.domain.delivery.Channel;
import vn.marketplace.notification.domain.delivery.NotificationId;
import vn.marketplace.notification.domain.delivery.NotificationStatus;
import vn.marketplace.notification.domain.delivery.Priority;
import vn.marketplace.notification.domain.shared.NotificationDomainException;
import vn.marketplace.notification.domain.shared.NotificationErrorCode;

class NotificationTest {

    private Notification accepted() {
        return Notification.accept(new NotificationId("N-1"), "idem-1", "U-1", Channel.EMAIL,
                "order-completed", Map.of("orderId", "O-1"), Priority.TRANSACTIONAL, "OrderCompleted", "evt-1");
    }

    @Test
    void sendingBeforeRenderingThrows() { // TC-NOT-01: ACCEPTED → SENT skipping RENDERED
        Notification n = accepted();

        NotificationDomainException ex = assertThrows(NotificationDomainException.class,
                () -> n.recordSent("SES"));

        assertEquals(NotificationErrorCode.INVALID_TRANSITION, ex.notificationErrorCode());
        assertEquals(NotificationStatus.ACCEPTED, n.status());
    }

    @Test
    void happyPathRenderThenSend() {
        Notification n = accepted();

        n.markRendered();
        assertEquals(NotificationStatus.RENDERED, n.status());

        n.recordSent("SES");
        assertEquals(NotificationStatus.SENT, n.status());
        assertEquals(1, n.attempts().size());
        assertEquals("SUCCESS", n.attempts().get(0).result());
    }

    @Test
    void suppressFromAcceptedReachesTerminalState() {
        Notification n = accepted();

        n.suppress("user opted out");

        assertEquals(NotificationStatus.SUPPRESSED, n.status());
    }

    @Test
    void cannotTransitionFromTerminalState() {
        Notification n = accepted();
        n.suppress("opted out");

        assertThrows(NotificationDomainException.class, n::markRendered);
    }

    @Test
    void missingTemplateIsRejected() {
        NotificationDomainException ex = assertThrows(NotificationDomainException.class,
                () -> Notification.accept(new NotificationId("N-2"), "idem-2", "U-1", Channel.SMS,
                        "  ", Map.of(), Priority.BULK, null, null));
        assertEquals(NotificationErrorCode.MISSING_TEMPLATE, ex.notificationErrorCode());
    }
}
