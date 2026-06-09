package vn.marketplace.catalog.application.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.CatalogDomainException;
import vn.marketplace.catalog.domain.shared.CatalogErrorCode;
import vn.marketplace.catalog.domain.shared.Role;

class UpdateSkuPriceUcTest {

    private static final Actor OWNER = new Actor("M-1", Role.MERCHANT);
    private static final Actor OTHER_MERCHANT = new Actor("M-2", Role.MERCHANT);

    private InMemoryProductRepository repository;
    private CreateProductUc createUc;
    private UpdateSkuPriceUc updateUc;

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        createUc = new CreateProductUc(repository);
        updateUc = new UpdateSkuPriceUc(repository);
    }

    private String givenProduct() {
        String id = createUc.execute(CatalogTestData.createCmd("M-1", "SKU-1", 100_000)).id();
        DomainEventPublisher.clear();
        return id;
    }

    @Test
    void ownerCanChangePriceAndItIsPersisted() {
        String id = givenProduct();

        SkuPriceView view = updateUc.execute(new UpdateSkuPriceCmd(id, "SKU-1", 200_000, OWNER));

        assertEquals("SKU-1", view.skuCode());
        assertEquals(200_000, view.priceAmount());
        assertEquals(200_000,
                repository.store.get(id).findSku(new SkuCode("SKU-1")).orElseThrow().price().amount());
    }

    @Test
    void otherMerchantCannotChangePrice() {
        String id = givenProduct();

        CatalogDomainException ex = assertThrows(CatalogDomainException.class,
                () -> updateUc.execute(new UpdateSkuPriceCmd(id, "SKU-1", 200_000, OTHER_MERCHANT)));

        assertEquals(CatalogErrorCode.TENANT_MISMATCH, ex.catalogErrorCode());
        assertEquals(100_000,
                repository.store.get(id).findSku(new SkuCode("SKU-1")).orElseThrow().price().amount());
    }

    @Test
    void priceMustBePositive() {
        String id = givenProduct();

        CatalogDomainException ex = assertThrows(CatalogDomainException.class,
                () -> updateUc.execute(new UpdateSkuPriceCmd(id, "SKU-1", 0, OWNER)));

        assertEquals(CatalogErrorCode.INVALID_PRICE, ex.catalogErrorCode());
    }

    @Test
    void unknownSkuIsRejected() {
        String id = givenProduct();

        CatalogDomainException ex = assertThrows(CatalogDomainException.class,
                () -> updateUc.execute(new UpdateSkuPriceCmd(id, "SKU-MISSING", 200_000, OWNER)));

        assertEquals(CatalogErrorCode.PRODUCT_NOT_FOUND, ex.catalogErrorCode());
    }

    @Test
    void unknownProductIsRejected() {
        CatalogDomainException ex = assertThrows(CatalogDomainException.class,
                () -> updateUc.execute(new UpdateSkuPriceCmd("P-MISSING", "SKU-1", 200_000, OWNER)));

        assertEquals(CatalogErrorCode.PRODUCT_NOT_FOUND, ex.catalogErrorCode());
    }
}
