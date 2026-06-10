package vn.marketplace.payment.adapter.payment.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.payment.adapter.payment.dto.WebhookRequest;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;
import vn.marketplace.payment.application.payment.HandleWebhookCmd;

/**
 * Inbound webhook from the payment gateway — trust boundary B4: verify the HMAC signature over the
 * RAW body, then the anti-replay window, before any parsing-driven behaviour (TC-PAY-INT-01:
 * bad signature → 401; stale timestamp → 401; valid → 200 and the use case runs). The 401s are
 * boundary authentication, not business errors — domain exceptions still propagate to the
 * GlobalExceptionHandler untouched.
 */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentFacade facade;
    private final WebhookSignatureVerifier verifier;
    // Local mapper: the raw body must be parsed AFTER signature verification, outside MVC binding.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/webhook")
    public ResponseEntity<CommonHttpResponse> webhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) throws java.io.IOException {

        if (!verifier.validSignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonHttpResponse(HttpStatus.UNAUTHORIZED, "invalid signature"));
        }

        WebhookRequest request = objectMapper.readValue(rawBody, WebhookRequest.class);

        if (!verifier.fresh(request.timestamp())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonHttpResponse(HttpStatus.UNAUTHORIZED, "stale webhook (replay rejected)"));
        }

        facade.handleWebhook(new HandleWebhookCmd(request.gatewayTxnId(), request.orderRef(),
                request.amount(), request.status()));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "processed"));
    }
}
