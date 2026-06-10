package vn.marketplace.payment.adapter.payment.inbound.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;
import vn.marketplace.payment.adapter.payment.outbound.gateway.MerchantBankDirectory;
import vn.marketplace.payment.application.payment.InitEscrowCmd;
import vn.marketplace.payment.application.payment.InitEscrowResult;
import vn.marketplace.payment.application.payment.PaymentView;
import vn.marketplace.payment.application.payment.ProcessSettlementCmd;
import vn.marketplace.payment.application.payment.SettlementView;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * Controller test via {@code standaloneSetup} (no Spring context, no infra) + real facade + fake
 * use-cases + the real msfw GlobalExceptionHandler — status/body for init-escrow and the
 * OrderCompleted event stand-in, plus a domain NOT_FOUND mapping to 4xx without try/catch → 500.
 */
class PaymentControllerTest {

    private static final String ESCROW_BODY = """
            {
              "orderRef": "CHK-1",
              "amount": 300,
              "currency": "VND",
              "allocations": [
                {"orderId": "O-1", "merchantId": "M-1", "amount": 100},
                {"orderId": "O-2", "merchantId": "M-2", "amount": 200}
              ]
            }
            """;

    private static final String ORDER_COMPLETED_BODY = """
            {
              "eventId": "EVT-1",
              "orderId": "O-1",
              "merchantId": "M-1",
              "items": [{"sku": "SKU-1", "qty": 2, "priceSnapshot": 50}]
            }
            """;

    private InitEscrowCmd lastEscrowCmd;
    private ProcessSettlementCmd lastSettlementCmd;
    private boolean payoutRequested;
    private RuntimeException getPaymentThrows;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lastEscrowCmd = null;
        lastSettlementCmd = null;
        payoutRequested = false;
        getPaymentThrows = null;
        PaymentFacade facade = new PaymentFacade(
                cmd -> {
                    lastEscrowCmd = cmd;
                    return new InitEscrowResult("PAY-1", "https://pg.example.com/pay/PAY-1",
                            LocalDateTime.of(2026, 6, 10, 10, 15));
                },
                cmd -> { },
                cmd -> {
                    lastSettlementCmd = cmd;
                    return settlementView("PENDING");
                },
                cmd -> {
                    payoutRequested = true;
                    return settlementView("SUBMITTED");
                },
                cmd -> {
                    if (getPaymentThrows != null) {
                        throw getPaymentThrows;
                    }
                    return new PaymentView("PAY-1", cmd.orderRef(), "PENDING", 300, "VND",
                            "https://pg.example.com/pay/PAY-1", null, List.of());
                },
                new MerchantBankDirectory());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new InternalPaymentController(facade), new PaymentEventController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static SettlementView settlementView(String payoutStatus) {
        return new SettlementView("SET-1", "O-1", "M-1", 100, 2, 98, "VND", "COMPLETED",
                "s3://settlement-docs/settlements/O-1.json", "PO-1", payoutStatus);
    }

    @Test
    void initEscrowReturns201WithEscrowIdAndPaymentUrl() throws Exception {
        mockMvc.perform(post("/internal/payments/escrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ESCROW_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.escrowId").value("PAY-1"))
                .andExpect(jsonPath("$.data.paymentUrl").value("https://pg.example.com/pay/PAY-1"));

        assertEquals("CHK-1", lastEscrowCmd.orderRef());
        assertEquals(2, lastEscrowCmd.allocations().size());
    }

    @Test
    void orderCompletedEventSettlesAndPaysOut() throws Exception {
        mockMvc.perform(post("/internal/events/order-completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_COMPLETED_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payoutStatus").value("SUBMITTED"));

        assertEquals("O-1", lastSettlementCmd.orderId());
        assertEquals(1, lastSettlementCmd.items().size());
        assertEquals(new MerchantBankDirectory().bankAccountFor("M-1"),
                lastSettlementCmd.merchantBankAccount(), "bank account resolved from the directory");
        assertEquals(true, payoutRequested);
    }

    @Test
    void unknownOrderRefMapsToClientErrorViaGlobalHandler() throws Exception {
        getPaymentThrows = new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND);

        mockMvc.perform(get("/internal/payments/CHK-404"))
                .andExpect(status().is4xxClientError());
    }
}
