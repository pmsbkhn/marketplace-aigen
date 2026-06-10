package vn.marketplace.notification.adapter.delivery.inbound.restful;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.notification.adapter.delivery.dto.OrderCompletedEvent;
import vn.marketplace.notification.adapter.delivery.dto.PaymentReceivedEvent;
import vn.marketplace.notification.adapter.delivery.facade.NotificationFacade;
import vn.marketplace.notification.application.delivery.AcceptNotificationCmd;

/**
 * Inbound business-event handlers. In production these are Kafka consumers (OrderCompleted,
 * PaymentReceived, …) via an msfw {@code FiveStepsPipelineFactory} that map an event to one or more
 * notifications; here they are REST stand-ins (accept + dispatch synchronously). Idempotency key is
 * {@code {eventType}:{eventId}:{recipientRole}}.
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class NotificationEventController {

    private final NotificationFacade facade;

    @PostMapping("/order-completed")
    public ResponseEntity<CommonHttpResponse> orderCompleted(@RequestBody OrderCompletedEvent event) {
        facade.acceptAndDispatch(new AcceptNotificationCmd(
                "OrderCompleted:" + event.eventId() + ":buyer", event.buyerId(), "EMAIL", "order-completed",
                Map.of("orderId", event.orderId()), "TRANSACTIONAL", "OrderCompleted", event.eventId()));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "notified"));
    }

    @PostMapping("/payment-received")
    public ResponseEntity<CommonHttpResponse> paymentReceived(@RequestBody PaymentReceivedEvent event) {
        facade.acceptAndDispatch(new AcceptNotificationCmd(
                "PaymentReceived:" + event.eventId() + ":buyer", event.buyerId(), "EMAIL", "payment-received",
                Map.of("orderId", event.orderId(), "amount", String.valueOf(event.amount())),
                "TRANSACTIONAL", "PaymentReceived", event.eventId()));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "notified"));
    }
}
