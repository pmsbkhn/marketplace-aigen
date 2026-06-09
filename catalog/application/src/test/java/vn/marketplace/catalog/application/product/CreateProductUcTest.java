package vn.marketplace.catalog.application.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.catalog.domain.product.ProductStatus;
import vn.marketplace.catalog.domain.product.management.Product;

class CreateProductUcTest {

    private InMemoryProductRepository repository;
    private CreateProductUc useCase;

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        useCase = new CreateProductUc(repository);
    }

    @Test
    void persistsAPendingProductAndReturnsItsId() {
        ProductIdView view = useCase.execute(CatalogTestData.createCmd("M-1", "SKU-1", 250_000));

        assertEquals("PENDING", view.status());
        assertEquals(1, repository.saved.size());
        Product saved = repository.saved.get(0);
        assertEquals(ProductStatus.PENDING, saved.status());
        assertEquals(view.id(), saved.id().value());
    }

    @Test
    void creationDoesNotPublishProductCreated() {
        useCase.execute(CatalogTestData.createCmd("M-1", "SKU-1", 250_000));

        assertTrue(DomainEventPublisher.getEvents().isEmpty(),
                "ProductCreated is published on first activation, not on creation");
    }
}
