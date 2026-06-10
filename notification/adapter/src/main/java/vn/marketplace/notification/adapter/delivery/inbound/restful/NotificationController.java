package vn.marketplace.notification.adapter.delivery.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.notification.adapter.delivery.dto.AcceptNotificationRequest;
import vn.marketplace.notification.adapter.delivery.facade.NotificationFacade;
import vn.marketplace.notification.application.delivery.AcceptNotificationCmd;
import vn.marketplace.notification.application.delivery.NotificationIdView;
import vn.marketplace.notification.application.delivery.NotificationStatusView;

/**
 * Notification ingest + status. Other services call POST to request a notification (async accept);
 * the sender reads back delivery status via GET. No try/catch → 500.
 */
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationFacade facade;

    @PostMapping
    public ResponseEntity<CommonHttpResponse> accept(
            @RequestBody AcceptNotificationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String senderId) {
        NotificationIdView view = facade.accept(new AcceptNotificationCmd(
                request.idempotencyKey(), request.userId(), request.channel(), request.templateId(),
                request.variables(), request.priority(), "API", senderId));
        return ResponseEntity.accepted().body(new CommonHttpResponse(HttpStatus.ACCEPTED, view));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonHttpResponse> get(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String callerId) {
        NotificationStatusView view = facade.get(id, callerId);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }
}
