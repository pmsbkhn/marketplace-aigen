package vn.marketplace.order.adapter.order.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.order.adapter.order.dto.PaymentFailedEvent;
import vn.marketplace.order.adapter.order.dto.PaymentReceivedEvent;
import vn.marketplace.order.adapter.order.facade.OrderFacade;

/**
 * Inbound payment-event handlers. In production these are Kafka consumers ({@code PaymentReceived} /
 * {@code PaymentFailed} from Payment) via an msfw {@code FiveStepsPipelineFactory}; here they are REST
 * stand-ins so the flow is exercisable on the standalone profile.
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class OrderEventController {

    private final OrderFacade facade;

    @PostMapping("/payment-received")
    public ResponseEntity<CommonHttpResponse> paymentReceived(@RequestBody PaymentReceivedEvent event) {
        facade.paymentReceived(event.orderId(), event.eventId());
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "to_ship"));
    }

    @PostMapping("/payment-failed")
    public ResponseEntity<CommonHttpResponse> paymentFailed(@RequestBody PaymentFailedEvent event) {
        facade.cancelBySystem(event.orderId(), event.reason());
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "cancelled"));
    }
}
