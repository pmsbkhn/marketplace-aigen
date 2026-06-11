package vn.marketplace.catalog.adapter.product.management.outbound.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.catalog.adapter.product.management.outbound.persistence.entity.ProductEntity;

/**
 * Spring Data contract behind {@code ProductOa}. Identity and root-attribute lookups go through the
 * inherited Specification executor ({@code Criteria} DSL); the only hand-written queries are the ones
 * the translator cannot express — joining the variants/skus collections.
 */
public interface ProductJpaRepository extends JpaOaRepository<ProductEntity> {

    @Query("select distinct p from ProductEntity p join p.variants v join v.skus s where s.skuCode in :codes")
    List<ProductEntity> findBySkuCodes(@Param("codes") Collection<String> codes);

    /**
     * Paged storefront search over ACTIVE products (DB stand-in for Elasticsearch). Null parameter =
     * no filter on that dimension; {@code text} is a pre-lowercased {@code %needle%} pattern; a
     * product matches a price bound when ANY of its SKUs falls inside the range.
     */
    @Query(value = """
            select p from ProductEntity p
            where p.status = 'ACTIVE'
              and (:text is null
                   or lower(p.name) like :text
                   or lower(coalesce(p.description, '')) like :text)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:brandId is null or p.brandId = :brandId)
              and ((:priceMin is null and :priceMax is null) or exists (
                  select s from p.variants v join v.skus s
                  where (:priceMin is null or s.priceAmount >= :priceMin)
                    and (:priceMax is null or s.priceAmount <= :priceMax)))
            order by p.id
            """,
            countQuery = """
            select count(p) from ProductEntity p
            where p.status = 'ACTIVE'
              and (:text is null
                   or lower(p.name) like :text
                   or lower(coalesce(p.description, '')) like :text)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:brandId is null or p.brandId = :brandId)
              and ((:priceMin is null and :priceMax is null) or exists (
                  select s from p.variants v join v.skus s
                  where (:priceMin is null or s.priceAmount >= :priceMin)
                    and (:priceMax is null or s.priceAmount <= :priceMax)))
            """)
    Page<ProductEntity> searchActive(@Param("text") String text,
                                     @Param("categoryId") String categoryId,
                                     @Param("brandId") String brandId,
                                     @Param("priceMin") Long priceMin,
                                     @Param("priceMax") Long priceMax,
                                     Pageable pageable);
}
