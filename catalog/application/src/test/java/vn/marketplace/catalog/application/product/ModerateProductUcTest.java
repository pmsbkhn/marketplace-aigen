package vn.marketplace.catalog.application.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.ProductCreated;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;
import vn.marketplace.catalog.domain.shared.Role;

class ModerateProductUcTest {

    private static final Actor ADMIN = new Actor("admin-1", Role.ADMIN);
    private static final Actor MERCHANT = new Actor("M-1", Role.MERCHANT);

    private InMemoryProductRepository repository;
    private CreateProductUc createUc;
    private ModerateProductUc moderateUc;

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        createUc = new CreateProductUc(repository);
        moderateUc = new ModerateProductUc(repository);
    }

    private String givenPendingProduct() {
        String id = createUc.execute(CatalogTestData.createCmd("M-1", "SKU-1", 250_000)).id();
        DomainEventPublisher.clear();
        return id;
    }

    @Test
    void adminApprovalActivatesAndPublishesProductCreated() {
        String id = givenPendingProduct();

        ProductModerationView view = moderateUc.execute(
                new ModerateProductCmd(id, ADMIN, ModerationAction.APPROVE, null));

        assertEquals("ACTIVE", view.status());
        assertEquals("admin-1", view.moderatedBy());
        Product stored = repository.store.get(id);
        assertEquals("ACTIVE", stored.status().name());

        assertEquals(1, DomainEventPublisher.getEvents().size());
        assertInstanceOf(ProductCreated.class, DomainEventPublisher.getEvents().get(0));
    }

    @Test
    void adminRejectionMovesToRejectedWithReason() {
        String id = givenPendingProduct();

        ProductModerationView view = moderateUc.execute(
                new ModerateProductCmd(id, ADMIN, ModerationAction.REJECT, "blurry images"));

        assertEquals("REJECTED", view.status());
        assertEquals("blurry images", view.moderationReason());
        assertEquals("REJECTED", repository.store.get(id).status().name());
    }

    @Test
    void nonAdminCannotModerate() {
        String id = givenPendingProduct();

        CatalogDomainException ex = assertThrows(CatalogDomainException.class,
                () -> moderateUc.execute(new ModerateProductCmd(id, MERCHANT, ModerationAction.APPROVE, null)));
        assertEquals(CatalogErrorCode.UNAUTHORIZED_ACTION, ex.catalogErrorCode());
        assertEquals("PENDING", repository.store.get(id).status().name());
    }
}
