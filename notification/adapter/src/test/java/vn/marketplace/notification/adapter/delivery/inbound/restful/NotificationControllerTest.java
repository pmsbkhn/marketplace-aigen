package vn.marketplace.notification.adapter.delivery.inbound.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.notification.adapter.delivery.facade.NotificationFacade;
import vn.marketplace.notification.application.delivery.AcceptNotification;
import vn.marketplace.notification.application.delivery.AcceptNotificationCmd;
import vn.marketplace.notification.application.delivery.DispatchNotification;
import vn.marketplace.notification.application.delivery.DispatchNotificationCmd;
import vn.marketplace.notification.application.delivery.GetNotification;
import vn.marketplace.notification.application.delivery.GetNotificationCmd;
import vn.marketplace.notification.application.delivery.NotificationIdView;
import vn.marketplace.notification.application.delivery.NotificationStatusView;
import vn.marketplace.notification.domain.shared.NotificationDomainException;
import vn.marketplace.notification.domain.shared.NotificationErrorCode;

/**
 * Controller test via {@code standaloneSetup} (no Spring context, no infra) + real facade + fake
 * use-cases + the real msfw GlobalExceptionHandler — proving status/body, that an event triggers
 * accept+dispatch, and that a not-found maps to a 4xx without any try/catch → 500.
 */
class NotificationControllerTest {

    private FakeAccept accept;
    private FakeDispatch dispatch;
    private FakeGet get;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accept = new FakeAccept();
        dispatch = new FakeDispatch();
        get = new FakeGet();
        NotificationFacade facade = new NotificationFacade(accept, dispatch, get);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new NotificationController(facade), new NotificationEventController(facade),
                        new InternalDispatchController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void acceptReturns202() throws Exception {
        mockMvc.perform(post("/v1/notifications")
                        .header("X-User-Id", "svc-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"k1","userId":"U-1","channel":"EMAIL",
                                 "templateId":"order-completed","variables":{"orderId":"O-1"},"priority":"TRANSACTIONAL"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.notificationId").value("N-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void orderCompletedEventAcceptsAndDispatches() throws Exception {
        mockMvc.perform(post("/internal/events/order-completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"e1\",\"orderId\":\"O-1\",\"buyerId\":\"U-1\",\"merchantId\":\"M-1\"}"))
                .andExpect(status().isOk());

        Assertions.assertEquals("U-1", accept.lastCmd.userId());
        Assertions.assertEquals("order-completed", accept.lastCmd.templateId());
        Assertions.assertEquals(1, dispatch.calls);
    }

    @Test
    void getMissingNotificationMapsToClientError() throws Exception {
        get.toThrow = new NotificationDomainException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);

        mockMvc.perform(get("/v1/notifications/does-not-exist").header("X-User-Id", "svc-order"))
                .andExpect(status().is4xxClientError());
    }

    private static final class FakeAccept implements AcceptNotification {
        AcceptNotificationCmd lastCmd;

        @Override
        public NotificationIdView execute(AcceptNotificationCmd cmd) {
            this.lastCmd = cmd;
            return new NotificationIdView("N-1", "ACCEPTED");
        }
    }

    private static final class FakeDispatch implements DispatchNotification {
        int calls;

        @Override
        public void execute(DispatchNotificationCmd cmd) {
            calls++;
        }
    }

    private static final class FakeGet implements GetNotification {
        RuntimeException toThrow;

        @Override
        public NotificationStatusView execute(GetNotificationCmd cmd) {
            if (toThrow != null) {
                throw toThrow;
            }
            return new NotificationStatusView(cmd.notificationId(), "SENT", 1);
        }
    }
}
