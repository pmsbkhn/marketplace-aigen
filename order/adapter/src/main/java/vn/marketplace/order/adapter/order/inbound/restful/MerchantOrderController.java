package vn.marketplace.order.adapter.order.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.order.adapter.order.dto.ShipOrderRequest;
import vn.marketplace.order.adapter.order.facade.OrderFacade;

/** Merchant fulfilment endpoint (TO_SHIP → SHIPPED). No try/catch → 500. */
@RestController
@RequestMapping("/v1/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderFacade facade;

    @PostMapping("/{id}/ship")
    public ResponseEntity<CommonHttpResponse> ship(
            @PathVariable String id,
            @RequestBody ShipOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        facade.ship(id, request.trackingNumber(), RequestActor.from(userId, role));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "shipped"));
    }
}
