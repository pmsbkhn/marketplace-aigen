package vn.marketplace.notification.adapter.delivery.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.notification.adapter.delivery.facade.NotificationFacade;

/**
 * Internal dispatch trigger. In production the dispatcher is a worker draining the outbox/queue; this
 * REST endpoint is the stand-in to render + send one notification on the standalone profile.
 */
@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalDispatchController {

    private final NotificationFacade facade;

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<CommonHttpResponse> dispatch(@PathVariable String id) {
        facade.dispatch(id);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "dispatched"));
    }
}
