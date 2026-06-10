package vn.marketplace.checkout.adapter.checkout.inbound.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.checkout.adapter.checkout.facade.CheckoutFacade;
import vn.marketplace.checkout.application.checkout.CheckoutResultView;
import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;
import vn.marketplace.checkout.application.checkout.GetCheckoutSession;
import vn.marketplace.checkout.application.checkout.SubmitCheckout;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

/**
 * Controller test via {@code standaloneSetup} (no Spring context, no infra) + real facade + fake
 * use-cases + the real msfw GlobalExceptionHandler — status/body, header-derived identity, and
 * domain errors (out-of-stock, anti-IDOR session read) mapping to 4xx without try/catch → 500.
 */
class CheckoutControllerTest {

    private static final String BODY = """
            {
              "shippingAddress": {"fullName":"A","phone":"09","addressLine":"1 Le Loi","ward":"BN","district":"Q1","city":"HCMC"},
              "items": [
                {"sku": "SKU-A", "quantity": 2, "merchantId": "M-1"},
                {"sku": "SKU-B", "quantity": 1, "merchantId": "M-2"}
              ]
            }
            """;

    private FakeSubmit submit;
    private FakeGetSession getSession;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        submit = new FakeSubmit();
        getSession = new FakeGetSession();
        CheckoutFacade facade = new CheckoutFacade(submit, getSession);
        mockMvc = MockMvcBuilders.standaloneSetup(new CheckoutController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitReturnsPaymentUrlAndOrderIds() throws Exception {
        mockMvc.perform(post("/v1/checkout")
                        .header("X-Idempotency-Key", "IK-1")
                        .header("X-User-Id", "B-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentUrl").value("https://pg.example.com/pay/IK-1"))
                .andExpect(jsonPath("$.data.orderIds[0]").value("O-1"))
                .andExpect(jsonPath("$.data.grandTotalAmount").value(250));

        assertEquals("IK-1", submit.lastCmd.idempotencyKey());
        assertEquals("B-1", submit.lastCmd.buyerId(), "identity from header, never from body");
        assertEquals(2, submit.lastCmd.items().size());
    }

    @Test
    void missingIdempotencyKeyHeaderIsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outOfStockMapsToClientErrorViaGlobalHandler() throws Exception {
        submit.toThrow = new CheckoutDomainException(CheckoutErrorCode.OUT_OF_STOCK);

        mockMvc.perform(post("/v1/checkout")
                        .header("X-Idempotency-Key", "IK-1")
                        .header("X-User-Id", "B-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void foreignSessionReadMapsToClientError_antiIdor() throws Exception {
        getSession.toThrow = new CheckoutDomainException(CheckoutErrorCode.SESSION_NOT_FOUND);

        mockMvc.perform(get("/v1/checkout/IK-1").header("X-User-Id", "B-2"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void ownerSessionReadReturnsCachedResult() throws Exception {
        mockMvc.perform(get("/v1/checkout/IK-1").header("X-User-Id", "B-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("REDIRECTED"))
                .andExpect(jsonPath("$.data.paymentUrl").value("url"));
    }

    private static final class FakeSubmit implements SubmitCheckout {
        SubmitCheckoutCmd lastCmd;
        RuntimeException toThrow;

        @Override
        public CheckoutResultView execute(SubmitCheckoutCmd cmd) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.lastCmd = cmd;
            return new CheckoutResultView("https://pg.example.com/pay/" + cmd.idempotencyKey(),
                    List.of("O-1", "O-2"), 250);
        }
    }

    private static final class FakeGetSession implements GetCheckoutSession {
        RuntimeException toThrow;

        @Override
        public CheckoutSession execute(String idempotencyKey, String callerBuyerId) {
            if (toThrow != null) {
                throw toThrow;
            }
            return new CheckoutSession(idempotencyKey, callerBuyerId,
                    CheckoutSession.STATE_REDIRECTED, List.of("O-1"), "url", 250, null);
        }
    }
}
