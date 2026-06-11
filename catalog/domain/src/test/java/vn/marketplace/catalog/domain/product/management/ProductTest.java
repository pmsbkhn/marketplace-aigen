package vn.marketplace.catalog.domain.product.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.catalog.domain.product.BrandId;
import vn.marketplace.catalog.domain.product.CategoryId;
import vn.marketplace.catalog.domain.product.Currency;
import vn.marketplace.catalog.domain.product.MerchantId;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.ProductStatus;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.SkuId;
import vn.marketplace.catalog.domain.product.VariantId;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;
import vn.marketplace.catalog.domain.shared.Role;

/**
 * Domain acceptance tests for the Catalog product aggregate — TC-CAT-01..05.
 */
class ProductTest {

    private static final ProductFactory FACTORY = new ProductFactory();
    private static final Actor ADMIN = new Actor("admin-1", Role.ADMIN);
    private static final Actor MERCHANT = new Actor("M-123", Role.MERCHANT);

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private ProductFactoryParams params(String merchantId, long price) {
        return new ProductFactoryParams(
                new ProductId("P-1"),
                new MerchantId(merchantId),
                "T-Shirt",
                "A plain t-shirt",
                new BrandId("B-1"),
                new CategoryId("C-1"),
                "creator-1",
                List.of(new ProductFactoryParams.VariantSpec(
                        new VariantId("V-1"), "red-M", Map.of("color", "red", "size", "M"),
                        List.of(new ProductFactoryParams.SkuSpec(
                                new SkuId("S-1"), new SkuCode("SKU-1"), price, Currency.VND)))),
                List.of());
    }

    @Test
    @DisplayName("TC-CAT-01 [Product Lifecycle]: a newly created product defaults to PENDING")
    void newProductIsPending() {
        Product product = FACTORY.create(params("M-123", 250_000));

        assertEquals(ProductStatus.PENDING, product.status());
    }

    @Test
    @DisplayName("TC-CAT-02 [Admin Authority]: only an Admin may approve PENDING → ACTIVE")
    void onlyAdminCanApprove() {
        Product approvable = FACTORY.create(params("M-123", 250_000));
        approvable.approve(ADMIN);
        assertEquals(ProductStatus.ACTIVE, approvable.status());

        Product other = FACTORY.create(params("M-123", 250_000));
        CatalogDomainException ex = assertThrows(CatalogDomainException.class, () -> other.approve(MERCHANT));
        assertEquals(CatalogErrorCode.UNAUTHORIZED_ACTION, ex.catalogErrorCode());
        assertEquals(ProductStatus.PENDING, other.status(), "rejected authority must not change state");
    }

    @Test
    @DisplayName("TC-CAT-03 [Strict Pricing]: a SKU price of 0 or negative is rejected with INVALID_PRICE")
    void nonPositivePriceIsRejected() {
        CatalogDomainException zero = assertThrows(CatalogDomainException.class,
                () -> FACTORY.create(params("M-123", 0)));
        assertEquals(CatalogErrorCode.INVALID_PRICE, zero.catalogErrorCode());

        CatalogDomainException negative = assertThrows(CatalogDomainException.class,
                () -> FACTORY.create(params("M-123", -100)));
        assertEquals(CatalogErrorCode.INVALID_PRICE, negative.catalogErrorCode());
    }

    @Test
    @DisplayName("TC-CAT-04 [Tenant Immutability]: there is no API to change merchantId after creation")
    void merchantIdIsImmutable() throws NoSuchFieldException {
        Product product = FACTORY.create(params("M-123", 250_000));
        assertEquals("M-123", product.merchantId().value());

        // The field is final...
        Field merchantField = Product.class.getDeclaredField("merchantId");
        assertTrue(Modifier.isFinal(merchantField.getModifiers()),
                "merchantId field must be final");

        // ...and no public method exposes a way to set/replace the tenant.
        boolean anyTenantMutator = Arrays.stream(Product.class.getMethods())
                .anyMatch(m -> Arrays.asList(m.getParameterTypes()).contains(MerchantId.class));
        assertFalse(anyTenantMutator, "No public method may accept a MerchantId (tenant is immutable)");

        // State-changing operations leave the tenant untouched.
        product.approve(ADMIN);
        product.deactivate();
        assertEquals("M-123", product.merchantId().value());
    }

    @Test
    @DisplayName("TC-CAT-05 [Event Generation]: approving to ACTIVE publishes ProductCreated exactly once")
    void approvalPublishesProductCreated() {
        Product product = FACTORY.create(params("M-123", 250_000));
        assertTrue(DomainEventPublisher.getEvents().isEmpty(), "creation publishes nothing");

        product.approve(ADMIN);

        List<?> events = DomainEventPublisher.getEvents();
        assertEquals(1, events.size());
        ProductCreated event = assertInstanceOf(ProductCreated.class, events.get(0));
        assertEquals("P-1", event.productId());
        assertEquals(List.of("SKU-1"), event.skuCodes());
    }

    @Test
    @DisplayName("TC-CAT-05 (cont.): once published, a later activation does NOT re-publish ProductCreated")
    void productCreatedPublishedOnlyOnce() {
        // A product reconstructed from persistence that has ALREADY published ProductCreated
        // (product_created_published = true) but is back in PENDING. Re-activating must not re-publish
        // (ADR-3 — Inventory already initialised the SKUs).
        LocalDateTime now = LocalDateTime.now();
        Product.Memento memento = new Product.Memento(
                1L, "P-1", "M-123", "T-Shirt", "desc", "PENDING", "B-1", "C-1",
                true, "creator", null, null, now, now, null,
                List.of(new Product.VariantMemento(10L, "V-1", "red-M", Map.of(),
                        List.of(new Product.SkuMemento(100L, "S-1", "SKU-1", 250_000, "VND")))),
                List.of());
        Product product = Product.restore(memento);
        DomainEventPublisher.clear();

        product.approve(ADMIN);

        assertTrue(DomainEventPublisher.getEvents().isEmpty(),
                "ProductCreated must be published only on the first activation");
        assertTrue(product.productCreatedPublished());
    }
}
