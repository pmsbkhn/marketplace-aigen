package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import vn.marketplace.catalog.adapter.product.management.facade.ProductFacade;
import vn.marketplace.catalog.application.product.CreateProduct;
import vn.marketplace.catalog.application.product.CreateProductCmd;
import vn.marketplace.catalog.application.product.GetProduct;
import vn.marketplace.catalog.application.product.GetProductCmd;
import vn.marketplace.catalog.application.product.ProductIdView;
import vn.marketplace.catalog.application.product.ProductSearchResult;
import vn.marketplace.catalog.application.product.ProductView;
import vn.marketplace.catalog.application.product.SearchProducts;
import vn.marketplace.catalog.application.product.SearchProductsCmd;
import vn.marketplace.catalog.application.product.GetPrice;
import vn.marketplace.catalog.application.product.GetPriceCmd;
import vn.marketplace.catalog.application.product.SkuPriceView;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;

/**
 * Controller test via {@code standaloneSetup} (no Spring Boot context, no infra) + real facade + fake
 * use-cases + the real msfw GlobalExceptionHandler — proving status/body and that domain exceptions map
 * to HTTP without any try/catch → 500 in the controller.
 */
class ProductControllerTest {

    private static final String CREATE_BODY = """
            {
              "name": "T-Shirt",
              "brandId": "B-1",
              "categoryId": "C-1",
              "description": "A plain t-shirt",
              "variants": [
                {"name": "red-M", "attributes": {"color": "red"},
                 "skus": [{"skuCode": "SKU-1", "priceAmount": 250000, "currency": "VND"}]}
              ],
              "images": []
            }
            """;

    private FakeCreateProduct createProduct;
    private FakeGetProduct getProduct;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createProduct = new FakeCreateProduct();
        getProduct = new FakeGetProduct();
        ProductFacade facade = new ProductFacade(createProduct, getProduct,
                cmd -> new ProductSearchResult(List.of(), 0, 0, 24),
                cmd -> List.of(),
                cmd -> new SkuPriceView(cmd.skuCode(), cmd.newAmount(), "VND", "M-1", true));
        ProductController controller = new ProductController(facade);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201WithProductId() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .header("X-User-Id", "M-123")
                        .header("X-User-Role", "MERCHANT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("P-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        org.junit.jupiter.api.Assertions.assertEquals("M-123", createProduct.lastCmd.merchantId());
    }

    @Test
    void getMissingProductMapsToClientErrorViaGlobalHandler() throws Exception {
        getProduct.toThrow = new CatalogDomainException(CatalogErrorCode.PRODUCT_NOT_FOUND);

        mockMvc.perform(get("/v1/products/does-not-exist")
                        .header("X-User-Id", "buyer-1")
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateSkuPriceReturnsUpdatedPrice() throws Exception {
        mockMvc.perform(put("/v1/products/P-1/skus/SKU-1")
                        .header("X-User-Id", "M-1")
                        .header("X-User-Role", "MERCHANT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priceAmount\": 200000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuCode").value("SKU-1"))
                .andExpect(jsonPath("$.data.priceAmount").value(200000));
    }

    // ---- fakes ----

    private static final class FakeCreateProduct implements CreateProduct {
        CreateProductCmd lastCmd;

        @Override
        public ProductIdView execute(CreateProductCmd cmd) {
            this.lastCmd = cmd;
            return new ProductIdView("P-1", "PENDING");
        }
    }

    private static final class FakeGetProduct implements GetProduct {
        RuntimeException toThrow;

        @Override
        public ProductView execute(GetProductCmd cmd) {
            if (toThrow != null) {
                throw toThrow;
            }
            return new ProductView(cmd.productId(), "M-1", "T-Shirt", "desc", "ACTIVE",
                    "B-1", "C-1", List.of(), List.of(), null, null, null, null);
        }
    }
}
