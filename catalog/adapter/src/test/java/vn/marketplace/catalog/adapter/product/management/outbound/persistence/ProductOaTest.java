package vn.marketplace.catalog.adapter.product.management.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductEntity;
import vn.marketplace.catalog.domain.product.BrandId;
import vn.marketplace.catalog.domain.product.CategoryId;
import vn.marketplace.catalog.domain.product.Currency;
import vn.marketplace.catalog.domain.product.MerchantId;
import vn.marketplace.catalog.domain.product.ProductId;
import vn.marketplace.catalog.domain.product.ProductImage;
import vn.marketplace.catalog.domain.product.SkuCode;
import vn.marketplace.catalog.domain.product.SkuId;
import vn.marketplace.catalog.domain.product.VariantId;
import vn.marketplace.catalog.domain.product.management.Product;
import vn.marketplace.catalog.domain.product.management.ProductFactory;
import vn.marketplace.catalog.domain.product.management.ProductFactoryParams;
import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.Role;

/**
 * H2-backed pinning tests for {@link ProductOa} on the msfw {@code AbstractMementoJpaOa} base:
 * aggregate-graph round trip, merge-in-place of the variant/sku children on update (surrogate keys
 * stable — no delete + reinsert that would trip the unique {@code sku_code} index), upsert by
 * {@code productId} for fresh aggregates, the collection-join finder, and real DB paging in
 * {@code searchActive}.
 */
@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@Import(ProductOa.class)
class ProductOaTest {

    static {
        // Legacy bootstrap (forced by spring-cloud-starter-bootstrap) would otherwise load
        // bootstrap.yml with a mandatory config-server import; the standalone variant is optional.
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-standalone");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ProductEntity.class)
    @EnableJpaRepositories(basePackageClasses = ProductJpaRepository.class)
    static class JpaTestConfig {
    }

    private static final ProductFactory FACTORY = new ProductFactory();
    private static final Actor ADMIN = new Actor("admin-1", Role.ADMIN);

    @Autowired
    private ProductOa productOa;

    @Autowired
    private ProductJpaRepository jpa;

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Product newProduct(String productId, String name, String skuCode, long price) {
        Product product = FACTORY.create(new ProductFactoryParams(
                new ProductId(productId),
                new MerchantId("M-1"),
                name,
                "A plain " + name.toLowerCase(),
                new BrandId("B-1"),
                new CategoryId("C-1"),
                "creator-1",
                List.of(new ProductFactoryParams.VariantSpec(
                        new VariantId(productId + "-V1"), "red-M", Map.of("color", "red", "size", "M"),
                        List.of(new ProductFactoryParams.SkuSpec(
                                new SkuId(productId + "-S1"), new SkuCode(skuCode), price, Currency.VND)))),
                List.of(new ProductImage("https://img/" + productId + ".jpg", 0))));
        DomainEventPublisher.clear();
        return product;
    }

    @Test
    void saveAndFindByIdRoundTripsTheAggregateGraph() {
        Product product = newProduct("P-1", "T-Shirt", "SKU-1", 250_000);

        productOa.save(product);

        assertNotNull(product._id(), "save must thread the surrogate id back");
        Product reloaded = productOa.findById(new ProductId("P-1")).orElseThrow();
        assertEquals("P-1", reloaded.id().value());
        assertEquals("M-1", reloaded.merchantId().value());
        assertEquals("T-Shirt", reloaded.name());
        assertEquals("PENDING", reloaded.status().name());
        assertEquals(1, reloaded.variants().size());
        assertEquals(Map.of("color", "red", "size", "M"), reloaded.variants().get(0).attributes());
        assertEquals(1, reloaded.variants().get(0).skus().size());
        assertEquals(250_000, reloaded.findSku(new SkuCode("SKU-1")).orElseThrow().price().amount());
        assertEquals(List.of(new ProductImage("https://img/P-1.jpg", 0)).get(0).url(),
                reloaded.images().get(0).url());
        assertNotNull(reloaded._id());
    }

    @Test
    void updateMergesChildCollectionsInPlaceWithoutDuplicatingRows() {
        Product product = newProduct("P-1", "T-Shirt", "SKU-1", 250_000);
        productOa.save(product);

        Product loaded = productOa.findById(new ProductId("P-1")).orElseThrow();
        Long variantRowId = loaded.variants().get(0)._id();
        Long skuRowId = loaded.variants().get(0).skus().get(0)._id();
        assertNotNull(variantRowId);
        assertNotNull(skuRowId);

        loaded.changeSkuPrice(new SkuCode("SKU-1"), 300_000);
        loaded.approve(ADMIN);
        DomainEventPublisher.clear();
        productOa.save(loaded);

        Product reloaded = productOa.findById(new ProductId("P-1")).orElseThrow();
        assertEquals(300_000, reloaded.findSku(new SkuCode("SKU-1")).orElseThrow().price().amount(),
                "the SKU price change must actually be persisted");
        assertEquals("ACTIVE", reloaded.status().name());
        assertEquals(1, reloaded.variants().size());
        assertEquals(1, reloaded.variants().get(0).skus().size());
        assertEquals(variantRowId, reloaded.variants().get(0)._id(),
                "variant row must be merged in place, not deleted + reinserted");
        assertEquals(skuRowId, reloaded.variants().get(0).skus().get(0)._id(),
                "sku row must be merged in place (unique sku_code index)");
        assertEquals(1, jpa.count());
    }

    @Test
    void savingAFreshAggregateForAnExistingProductIdUpsertsInsteadOfDuplicating() {
        productOa.save(newProduct("P-1", "T-Shirt", "SKU-1", 250_000));

        // Fresh aggregate (_id == null) with the same natural key — legacy adapter upserted.
        Product fresh = newProduct("P-1", "T-Shirt v2", "SKU-1", 990_000);
        productOa.save(fresh);

        assertEquals(1, jpa.count(), "same productId must update the existing row");
        Product reloaded = productOa.findById(new ProductId("P-1")).orElseThrow();
        assertEquals("T-Shirt v2", reloaded.name());
        assertEquals(990_000, reloaded.findSku(new SkuCode("SKU-1")).orElseThrow().price().amount());
    }

    @Test
    void findBySkuCodesJoinsTheChildCollections() {
        productOa.save(newProduct("P-1", "T-Shirt", "SKU-1", 250_000));
        productOa.save(newProduct("P-2", "Hoodie", "SKU-2", 500_000));

        List<Product> found = productOa.findBySkuCodes(List.of("SKU-2", "SKU-MISSING"));

        assertEquals(1, found.size());
        assertEquals("P-2", found.get(0).id().value());
        assertNotNull(found.get(0)._id(), "custom finders must reconstitute (thread _id)");
        assertEquals(List.of(), productOa.findBySkuCodes(List.of()));
    }

    @Test
    void searchActiveFiltersAndPagesInTheDatabase() {
        for (int i = 1; i <= 3; i++) {
            Product p = newProduct("P-" + i, "T-Shirt " + i, "SKU-" + i, 100_000L * i);
            p.approve(ADMIN);
            DomainEventPublisher.clear();
            productOa.save(p);
        }
        productOa.save(newProduct("P-9", "T-Shirt pending", "SKU-9", 100_000)); // stays PENDING

        // Only ACTIVE products, case-insensitive text match.
        PagedSearchResult<Product> all = productOa.searchActive(
                "t-shirt", null, null, null, null, Pagination.of(0, 10));
        assertEquals(3, all.content().size());

        // Price bound matches when ANY sku falls in range.
        PagedSearchResult<Product> priced = productOa.searchActive(
                null, null, null, 150_000L, 250_000L, Pagination.of(0, 10));
        assertEquals(List.of("P-2"), priced.content().stream().map(p -> p.id().value()).toList());

        // Non-matching category filters everything out.
        PagedSearchResult<Product> wrongCategory = productOa.searchActive(
                null, "C-OTHER", null, null, null, Pagination.of(0, 10));
        assertTrue(wrongCategory.content().isEmpty());

        // Real DB paging: page 0 of size 2 has a next page, page 1 is the last.
        PagedSearchResult<Product> page0 = productOa.searchActive(
                null, null, null, null, null, Pagination.of(0, 2));
        assertEquals(2, page0.content().size());
        assertEquals(Optional.of(1), page0.nextPage());
        PagedSearchResult<Product> page1 = productOa.searchActive(
                null, null, null, null, null, Pagination.of(1, 2));
        assertEquals(1, page1.content().size());
        assertEquals(Optional.empty(), page1.nextPage());
    }

    @Test
    void deleteRemovesTheRowByIdentity() {
        productOa.save(newProduct("P-1", "T-Shirt", "SKU-1", 250_000));

        productOa.delete(new ProductId("P-1"));

        assertEquals(Optional.empty(), productOa.findById(new ProductId("P-1")));
        assertEquals(0, jpa.count());
    }
}
