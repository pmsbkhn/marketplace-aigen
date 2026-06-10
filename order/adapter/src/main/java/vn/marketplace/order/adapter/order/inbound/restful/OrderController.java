package vn.marketplace.order.adapter.order.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.order.adapter.order.dto.CancelOrderRequest;
import vn.marketplace.order.adapter.order.facade.OrderFacade;
import vn.marketplace.order.application.order.OrderView;
import vn.marketplace.order.domain.shared.Actor;

/**
 * Buyer/Admin order endpoints. Visibility & state-machine rules are enforced in the domain; this
 * controller only forwards the verified actor. No try/catch → 500.
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade facade;

    @GetMapping("/{id}")
    public ResponseEntity<CommonHttpResponse> get(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        OrderView view = facade.get(id, RequestActor.from(userId, role));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }

    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<CommonHttpResponse> confirmDelivery(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        facade.confirmDelivery(id, RequestActor.from(userId, role));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "completed"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommonHttpResponse> cancel(
            @PathVariable String id,
            @RequestBody CancelOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Actor actor = RequestActor.from(userId, role);
        facade.cancel(id, request.reason(), actor);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "cancelled"));
    }
}
