package vn.marketplace.payment.adapter.payment.inbound.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;



import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;
import vn.marketplace.payment.adapter.payment.outbound.gateway.MerchantBankDirectory;
import vn.marketplace.payment.application.payment.HandleWebhook;
import vn.marketplace.payment.application.payment.HandleWebhookCmd;
import vn.marketplace.payment.application.payment.InitEscrowResult;
import vn.marketplace.payment.application.payment.PaymentView;
import vn.marketplace.payment.application.payment.SettlementView;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * TC-PAY-INT-01 — webhook trust boundary via {@code standaloneSetup} (no Spring context):
 * (A) wrong HMAC signature → 401; (B) valid signature but payload timestamp older than 5 minutes
 * (replay) → 401; (C) valid + fresh → 200 and the use case is invoked. Plus: a domain veto
 * (AMOUNT_MISMATCH) maps to a 4xx via the GlobalExceptionHandler — no try/catch → 500.
 */
class WebhookControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-10T10:00:00Z");

    private WebhookSignatureVerifier verifier;
    private FakeHandleWebhook handleWebhook;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        verifier = new WebhookSignatureVerifier("test-secret", Duration.ofMinutes(5),
                Clock.fixed(NOW, ZoneOffset.UTC));
        handleWebhook = new FakeHandleWebhook();
        PaymentFacade facade = new PaymentFacade(
                cmd -> new InitEscrowResult("PAY-1", "https://pg.example.com/pay/PAY-1", LocalDateTime.now()),
                handleWebhook,
                cmd -> settlementView(),
                cmd -> settlementView(),
                cmd -> new PaymentView("PAY-1", cmd.orderRef(), "PENDING", 0, "VND", null, null, List.of()),
                new MerchantBankDirectory());
        mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(facade, verifier))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static SettlementView settlementView() {
        return new SettlementView("SET-1", "O-1", "M-1", 100, 2, 98, "VND", "COMPLETED",
                "s3://settlement-docs/settlements/O-1.json", "PO-1", "SUBMITTED");
    }

    private String body(long epochSeconds) {
        return "{\"gatewayTxnId\":\"TXN-1\",\"orderRef\":\"CHK-1\",\"amount\":1500000,"
                + "\"status\":\"SUCCESS\",\"timestamp\":" + epochSeconds + "}";
    }

    @Test
    void caseA_wrongSignatureIsRejected401() throws Exception { // TC-PAY-INT-01 (A)
        String body = body(NOW.getEpochSecond());

        mockMvc.perform(post("/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", "deadbeef")
                        .content(body))
                .andExpect(status().isUnauthorized());

        assertNull(handleWebhook.lastCmd, "use case must NOT run on a forged webhook");
    }

    @Test
    void caseB_staleTimestampIsReplayRejected401() throws Exception { // TC-PAY-INT-01 (B)
        String body = body(NOW.minus(Duration.ofMinutes(6)).getEpochSecond()); // older than 5 min
        String validSignature = verifier.hmacHex(body);

        mockMvc.perform(post("/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", validSignature)
                        .content(body))
                .andExpect(status().isUnauthorized());

        assertNull(handleWebhook.lastCmd, "use case must NOT run on a replayed webhook");
    }

    @Test
    void caseC_validSignatureAndFreshTimestampRuns200() throws Exception { // TC-PAY-INT-01 (C)
        String body = body(NOW.minusSeconds(60).getEpochSecond());
        String validSignature = verifier.hmacHex(body);

        mockMvc.perform(post("/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", validSignature)
                        .content(body))
                .andExpect(status().isOk());

        assertEquals("TXN-1", handleWebhook.lastCmd.gatewayTxnId());
        assertEquals("CHK-1", handleWebhook.lastCmd.orderRef());
        assertEquals(1_500_000, handleWebhook.lastCmd.amount());
    }

    @Test
    void missingSignatureHeaderIsRejected401() throws Exception {
        mockMvc.perform(post("/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NOW.getEpochSecond())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void domainVetoMapsToClientErrorViaGlobalHandler() throws Exception {
        handleWebhook.toThrow = new PaymentDomainException(PaymentErrorCode.AMOUNT_MISMATCH);
        String body = body(NOW.getEpochSecond());

        mockMvc.perform(post("/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", verifier.hmacHex(body))
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    private static final class FakeHandleWebhook implements HandleWebhook {
        HandleWebhookCmd lastCmd;
        RuntimeException toThrow;

        @Override
        public void execute(HandleWebhookCmd cmd) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.lastCmd = cmd;
        }
    }
}
