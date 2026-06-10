package vn.marketplace.payment.adapter.payment.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.payment.adapter.payment.dto.InitEscrowRequest;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;
import vn.marketplace.payment.application.payment.InitEscrowCmd;
import vn.marketplace.payment.application.payment.InitEscrowCmd.EscrowAllocationInput;
import vn.marketplace.payment.application.payment.InitEscrowResult;

/**
 * Internal payment operations for Checkout. The SAD specifies gRPC {@code Payment.InitEscrow};
 * these REST endpoints are the transport stand-in for the standalone profile (consistent with
 * Catalog/Inventory/Order). No try/catch → 500.
 */
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentFacade facade;

    @PostMapping("/escrow")
    public ResponseEntity<CommonHttpResponse> initEscrow(@RequestBody InitEscrowRequest request) {
        var allocations = request.allocations() == null ? java.util.List.<EscrowAllocationInput>of()
                : request.allocations().stream()
                        .map(a -> new EscrowAllocationInput(a.orderId(), a.merchantId(), a.amount()))
                        .toList();
        InitEscrowResult result = facade.initEscrow(new InitEscrowCmd(
                request.orderRef(), request.amount(), request.currency(), allocations));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommonHttpResponse(HttpStatus.CREATED, result));
    }

    @GetMapping("/{orderRef}")
    public ResponseEntity<CommonHttpResponse> getByOrderRef(@PathVariable String orderRef) {
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, facade.getByOrderRef(orderRef)));
    }
}
