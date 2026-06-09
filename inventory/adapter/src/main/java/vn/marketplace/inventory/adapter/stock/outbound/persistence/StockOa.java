package vn.marketplace.inventory.adapter.stock.outbound.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.msfw.domain.core.PagedSearchResult;
import tech.vsf.ptnt.msfw.domain.core.Pagination;
import vn.marketplace.inventory.adapter.stock.outbound.persistence.entity.ReservationEntity;
import vn.marketplace.inventory.adapter.stock.outbound.persistence.entity.StockEntity;
import vn.marketplace.inventory.application.stock.StockCriteria;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Outbound persistence adapter: maps the {@link Stock} aggregate ↔ JPA entities via the aggregate
 * {@link Stock.Memento}, implementing the msfw {@code Repository<Stock>} port. {@code @Transactional}
 * keeps the session open while the lazy reservations collection is read during reconstruction.
 *
 * <p>On save the reservations collection is rebuilt from the memento (orphanRemoval syncs the table) —
 * reservations grow and change status over the aggregate's life, unlike Catalog's immutable variants.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class StockOa implements tech.vsf.ptnt.msfw.domain.core.Repository<Stock> {

    private final StockJpaRepository jpa;

    @Override
    public void save(Stock aggregate) {
        Stock.Memento m = aggregate.toMemento();
        StockEntity entity = jpa.findBySku(m.sku()).orElseGet(StockEntity::new);

        entity.setSku(m.sku());
        entity.setMerchantId(m.merchantId());
        entity.setAvailable(m.available());
        entity.setReserved(m.reserved());
        entity.setVersion(m.version());
        entity.setCreatedAt(m.createdAt());
        entity.setUpdatedAt(m.updatedAt());

        entity.getReservations().clear();
        for (Stock.ReservationMemento rm : m.reservations()) {
            ReservationEntity re = new ReservationEntity();
            re.setReservationId(rm.reservationId());
            re.setOrderRef(rm.orderRef());
            re.setSku(rm.sku());
            re.setQty(rm.qty());
            re.setStatus(rm.status());
            re.setExpiresAt(rm.expiresAt());
            entity.getReservations().add(re);
        }

        StockEntity saved = jpa.save(entity);
        aggregate.set_id(saved.getId());
    }

    @Override
    public <U extends Identity<?>> Optional<Stock> findById(U id) {
        return jpa.findBySku(String.valueOf(id.value())).map(this::toDomain);
    }

    @Override
    public List<Stock> findBy(Criteria criteria) {
        StockCriteria c = (StockCriteria) criteria;
        List<StockEntity> entities;
        if (c.skuCodes() != null) {
            entities = c.skuCodes().isEmpty() ? List.of() : jpa.findBySkuIn(c.skuCodes());
        } else if (c.orderRef() != null) {
            entities = jpa.findByOrderRef(c.orderRef());
        } else {
            entities = jpa.findAll();
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public PagedSearchResult<Stock> findBy(Criteria criteria, Pagination pagination) {
        List<Stock> all = findBy(criteria);
        int from = Math.min(pagination.offset(), all.size());
        int to = Math.min(from + pagination.size(), all.size());
        List<Stock> page = new ArrayList<>(all.subList(from, to));
        Optional<Integer> nextPage = to < all.size() ? Optional.of(pagination.page() + 1) : Optional.empty();
        return new PagedSearchResult<>(page, pagination.page(), pagination.size(), nextPage);
    }

    @Override
    public <U extends Identity<?>> void delete(U id) {
        jpa.deleteBySku(String.valueOf(id.value()));
    }

    private Stock toDomain(StockEntity e) {
        List<Stock.ReservationMemento> reservations = new ArrayList<>();
        for (ReservationEntity re : e.getReservations()) {
            reservations.add(new Stock.ReservationMemento(re.getReservationId(), re.getOrderRef(),
                    re.getSku(), re.getQty(), re.getStatus(), re.getExpiresAt()));
        }
        Stock.Memento m = new Stock.Memento(e.getId(), e.getSku(), e.getMerchantId(),
                e.getAvailable(), e.getReserved(), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt(), reservations);
        return Stock.fromMemento(m);
    }
}
