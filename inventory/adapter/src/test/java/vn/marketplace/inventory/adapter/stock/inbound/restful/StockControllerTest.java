package vn.marketplace.inventory.adapter.stock.inbound.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import vn.marketplace.inventory.adapter.stock.facade.StockFacade;
import vn.marketplace.inventory.application.stock.ReserveStockResult;
import vn.marketplace.inventory.application.stock.ReserveStockResult.ItemReserveDetail;
import vn.marketplace.inventory.application.stock.StockView;
import vn.marketplace.inventory.application.stock.UpdateMerchantStock;
import vn.marketplace.inventory.application.stock.UpdateMerchantStockCmd;

/**
 * Controller test via {@code standaloneSetup} (no Spring context, no infra) + real facade + fake
 * use-cases + the real msfw GlobalExceptionHandler — proving status/body and that the controllers
 * forward the right commands without any try/catch → 500.
 */
class StockControllerTest {

    private FakeUpdateMerchantStock updateStock;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        updateStock = new FakeUpdateMerchantStock();
        StockFacade facade = new StockFacade(
                cmd -> new ReserveStockResult(cmd.orderRef(), true,
                        cmd.items().stream()
                                .map(i -> new ItemReserveDetail(i.sku(), ItemReserveDetail.OK, 100))
                                .toList()),
                cmd -> { },
                cmd -> { },
                cmd -> { },
                cmd -> List.of(),
                updateStock);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MerchantStockController(facade), new InternalStockController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void merchantUpdateForwardsCallerIdAndReturnsStock() throws Exception {
        mockMvc.perform(put("/v1/merchant/stock/SKU-1")
                        .header("X-User-Id", "M-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": 50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-1"))
                .andExpect(jsonPath("$.data.available").value(50));

        Assertions.assertEquals("M-1", updateStock.lastCmd.callerMerchantId());
        Assertions.assertEquals(50, updateStock.lastCmd.available());
    }

    @Test
    void reserveReturnsAllReserved() throws Exception {
        mockMvc.perform(post("/internal/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderRef\": \"ORDER-1\", \"items\": [{\"sku\": \"SKU-1\", \"quantity\": 3}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allReserved").value(true))
                .andExpect(jsonPath("$.data.reservationId").value("ORDER-1"));
    }

    private static final class FakeUpdateMerchantStock implements UpdateMerchantStock {
        UpdateMerchantStockCmd lastCmd;

        @Override
        public StockView execute(UpdateMerchantStockCmd cmd) {
            this.lastCmd = cmd;
            return new StockView(cmd.sku(), cmd.available(), 0, cmd.callerMerchantId(), 1);
        }
    }
}
