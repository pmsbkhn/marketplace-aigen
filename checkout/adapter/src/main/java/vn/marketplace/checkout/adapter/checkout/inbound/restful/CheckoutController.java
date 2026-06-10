package vn.marketplace.checkout.adapter.checkout.inbound.restful;

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
import vn.marketplace.checkout.adapter.checkout.dto.CheckoutRequest;
import vn.marketplace.checkout.adapter.checkout.facade.CheckoutFacade;
import vn.marketplace.checkout.application.checkout.CheckoutResultView;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd.CheckoutItemInput;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd.ShippingAddressInput;

/**
 * Buyer-facing checkout endpoint. Identity comes from gateway-forwarded headers ({@code X-User-Id}
 * — the JWT is verified at the Gateway, never trusted from the body); the idempotency key is
 * REQUIRED via {@code X-Idempotency-Key}. No try/catch → 500 — domain exceptions propagate to the
 * GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutFacade facade;

    @PostMapping
    public ResponseEntity<CommonHttpResponse> submit(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody CheckoutRequest request) {
        CheckoutRequest.Address a = request.shippingAddress();
        ShippingAddressInput address = a == null ? null
                : new ShippingAddressInput(a.fullName(), a.phone(), a.addressLine(), a.ward(),
                        a.district(), a.city());
        var items = request.items() == null ? java.util.List.<CheckoutItemInput>of()
                : request.items().stream()
                        .map(i -> new CheckoutItemInput(i.sku(), i.quantity(), i.merchantId()))
                        .toList();
        String buyerId = (userId == null || userId.isBlank()) ? "anonymous" : userId;

        CheckoutResultView view = facade.submit(new SubmitCheckoutCmd(idempotencyKey, buyerId, address, items));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }

    @GetMapping("/{idempotencyKey}")
    public ResponseEntity<CommonHttpResponse> session(
            @PathVariable String idempotencyKey,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String buyerId = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK,
                facade.session(idempotencyKey, buyerId)));
    }
}
