package vn.marketplace.order.adapter.order.inbound.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.order.adapter.order.facade.OrderFacade;
import vn.marketplace.order.application.order.CreatePendingOrder;
import vn.marketplace.order.application.order.CreatePendingOrderCmd;
import vn.marketplace.order.application.order.GetOrder;
import vn.marketplace.order.application.order.GetOrderCmd;
import vn.marketplace.order.application.order.OrderIdView;
import vn.marketplace.order.application.order.OrderView;
import vn.marketplace.order.application.order.TransitionEvent;
import vn.marketplace.order.application.order.TransitionOrder;
import vn.marketplace.order.application.order.TransitionOrderCmd;
import vn.marketplace.order.domain.shared.OrderDomainException;
import vn.marketplace.order.domain.shared.OrderErrorCode;

/**
 * Controller test via {@code standaloneSetup} (no Spring context, no infra) + real facade + fake
 * use-cases + the real msfw GlobalExceptionHandler — proving status/body, the forwarded transition,
 * and that a cross-tenant 404 maps to a 4xx without any try/catch → 500.
 */
class OrderControllerTest {

    private static final String CREATE_BODY = """
            {
              "checkoutRef": "CHK-1",
              "buyerId": "B-1",
              "merchantId": "M-1",
              "shippingAddress": {"fullName":"A","phone":"09","addressLine":"1 Le Loi","ward":"BN","district":"Q1","city":"HCMC"},
              "items": [{"productId":"P-1","variantId":"V-1","skuCode":"SKU-1","productName":"Shirt","priceSnapshot":100,"currency":"VND","quantity":2}]
            }
            """;

    private FakeTransition transition;
    private FakeGetOrder getOrder;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transition = new FakeTransition();
        getOrder = new FakeGetOrder();
        OrderFacade facade = new OrderFacade(
                cmd -> new OrderIdView("O-1", "PENDING",
                        cmd.items().stream().mapToLong(i -> i.priceSnapshot() * i.quantity()).sum()),
                transition,
                cmd -> { },
                getOrder);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new OrderController(facade), new MerchantOrderController(facade), new InternalOrderController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201WithOrderIdAndTotal() throws Exception {
        mockMvc.perform(post("/internal/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value("O-1"))
                .andExpect(jsonPath("$.data.totalAmount").value(200));
    }

    @Test
    void shipForwardsMerchantTransitionWithTracking() throws Exception {
        mockMvc.perform(post("/v1/merchant/orders/O-1/ship")
                        .header("X-User-Id", "M-1")
                        .header("X-User-Role", "MERCHANT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\": \"TRACK-1\"}"))
                .andExpect(status().isOk());

        Assertions.assertEquals(TransitionEvent.MERCHANT_SHIP, transition.lastCmd.event());
        Assertions.assertEquals("TRACK-1", transition.lastCmd.trackingNumber());
    }

    @Test
    void crossTenantGetMapsToClientErrorViaGlobalHandler() throws Exception {
        getOrder.toThrow = new OrderDomainException(OrderErrorCode.ORDER_NOT_FOUND);

        mockMvc.perform(get("/v1/orders/O-1")
                        .header("X-User-Id", "B-2")
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().is4xxClientError());
    }

    private static final class FakeTransition implements TransitionOrder {
        TransitionOrderCmd lastCmd;

        @Override
        public void execute(TransitionOrderCmd cmd) {
            this.lastCmd = cmd;
        }
    }

    private static final class FakeGetOrder implements GetOrder {
        RuntimeException toThrow;

        @Override
        public OrderView execute(GetOrderCmd cmd) {
            if (toThrow != null) {
                throw toThrow;
            }
            return new OrderView(cmd.orderId(), "CHK-1", "B-1", "M-1", "PENDING", 200, "VND",
                    null, null, null, List.of(), List.of(), null, null);
        }
    }
}
