package vn.marketplace.payment.adapter.payment.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.payment.adapter.payment.dto.OrderCompletedEvent;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;

/**
 * Inbound order-event handler. In production this is a Kafka consumer ({@code OrderCompleted} from
 * OMS) via an msfw {@code FiveStepsPipelineFactory}; here it is a REST stand-in so the
 * settlement/payout flow is exercisable on the standalone profile. Idempotent end-to-end: the
 * settlement is keyed by {@code orderId} and the payout submit is a no-op on redelivery.
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class PaymentEventController {

    private final PaymentFacade facade;

    @PostMapping("/order-completed")
    public ResponseEntity<CommonHttpResponse> orderCompleted(@RequestBody OrderCompletedEvent event) {
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, facade.settleAndPayout(event)));
    }
}
