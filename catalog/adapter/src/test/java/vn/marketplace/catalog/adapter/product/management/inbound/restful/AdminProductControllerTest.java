package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.catalog.adapter.product.management.facade.ModerationFacade;
import vn.marketplace.catalog.application.product.ModerateProduct;
import vn.marketplace.catalog.application.product.ModerateProductCmd;
import vn.marketplace.catalog.application.product.ModerationAction;
import vn.marketplace.catalog.application.product.ProductModerationView;

/**
 * Admin moderation controller via {@code standaloneSetup} + real facade + fake use-case. Proves the
 * approve/reject routes forward the right {@link ModerationAction} and the verified actor, and map a
 * status/body without any try/catch → 500.
 */
class AdminProductControllerTest {

    private FakeModerateProduct moderate;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        moderate = new FakeModerateProduct();
        AdminProductController controller = new AdminProductController(new ModerationFacade(moderate));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void approveForwardsApproveActionAndModerator() throws Exception {
        mockMvc.perform(post("/v1/admin/products/P-1/approve")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Assertions.assertEquals(ModerationAction.APPROVE, moderate.lastCmd.action());
        Assertions.assertEquals("admin-1", moderate.lastCmd.moderator().id());
    }

    @Test
    void rejectForwardsRejectActionAndReason() throws Exception {
        mockMvc.perform(post("/v1/admin/products/P-1/reject")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"blurry images\"}"))
                .andExpect(status().isOk());

        Assertions.assertEquals(ModerationAction.REJECT, moderate.lastCmd.action());
        Assertions.assertEquals("blurry images", moderate.lastCmd.reason());
    }

    // ---- fake ----

    private static final class FakeModerateProduct implements ModerateProduct {
        ModerateProductCmd lastCmd;

        @Override
        public ProductModerationView execute(ModerateProductCmd cmd) {
            this.lastCmd = cmd;
            String status = cmd.action() == ModerationAction.APPROVE ? "ACTIVE" : "REJECTED";
            return new ProductModerationView(cmd.productId(), status,
                    cmd.moderator().id(), cmd.reason(), null);
        }
    }
}
