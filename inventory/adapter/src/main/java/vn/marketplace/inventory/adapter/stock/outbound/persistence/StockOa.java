package vn.marketplace.inventory.adapter.stock.outbound.persistence;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Identity;
import tech.vsf.ptnt.springcore.persistence.AbstractMementoJpaOa;
import tech.vsf.ptnt.springcore.persistence.JpaOaRepository;
import vn.marketplace.inventory.adapter.stock.outbound.persistence.entity.ReservationEntity;
import vn.marketplace.inventory.adapter.stock.outbound.persistence.entity.StockEntity;
import vn.marketplace.inventory.application.stock.StockRepository;
import vn.marketplace.inventory.domain.stock.management.Stock;

/**
 * Outbound persistence adapter: maps the {@link Stock} aggregate ↔ JPA entities at the
 * {@link Stock.Memento} level via msfw's {@code AbstractMementoJpaOa}, which owns the plumbing
 * (surrogate-key threading, identity lookups, {@code Criteria} → Specification translation, real
 * DB paging). {@code @Transactional} keeps the session open while the lazy reservations collection
 * is read during reconstruction.
 *
 * <p>On save the reservations collection is rebuilt from the memento (orphanRemoval syncs the
 * table) — reservations grow and change status over the aggregate's life. {@link #save} also
 * upserts by SKU: a fresh aggregate ({@code _id == null}) for an existing SKU updates that row
 * instead of violating the unique index, preserving the legacy adapter's semantics.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class StockOa extends AbstractMementoJpaOa<Stock, Stock.Memento, StockEntity>
        implements StockRepository {

    private final StockJpaRepository stockJpaRepository;

    @Override
    protected JpaOaRepository<StockEntity> jpa() {
        return stockJpaRepository;
    }

    @Override
    public void save(Stock aggregate) {
        if (aggregate._id() == null) {
            findEntity(aggregate.id()).ifPresent(e -> aggregate.set_id(e.getId()));
        }
        super.save(aggregate);
    }

    /** Joins the reservations collection — beyond the Criteria translator, so a derived query. */
    @Override
    public List<Stock> findByOrderRef(String orderRef) {
        return stockJpaRepository.findByOrderRef(orderRef).stream()
                .map(this::reconstitute)
                .toList();
    }

    @Override
    protected StockEntity fromMemento(Stock.Memento m) {
        StockEntity entity = new StockEntity();
        entity.setSku(m.sku());
        entity.setMerchantId(m.merchantId());
        entity.setAvailable(m.available());
        entity.setReserved(m.reserved());
        entity.setVersion(m.version());
        entity.setCreatedAt(m.createdAt());
        entity.setUpdatedAt(m.updatedAt());

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
        return entity;
    }

    @Override
    protected Stock.Memento toMemento(StockEntity e) {
        List<Stock.ReservationMemento> reservations = new ArrayList<>();
        for (ReservationEntity re : e.getReservations()) {
            reservations.add(new Stock.ReservationMemento(re.getReservationId(), re.getOrderRef(),
                    re.getSku(), re.getQty(), re.getStatus(), re.getExpiresAt()));
        }
        return new Stock.Memento(e.getId(), e.getSku(), e.getMerchantId(),
                e.getAvailable(), e.getReserved(), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt(), reservations);
    }

    @Override
    protected Stock restore(Stock.Memento memento) {
        return Stock.restore(memento);
    }

    @Override
    protected Criteria identityCriteria(Identity<?> id) {
        return Criteria.where("sku").eq(id.value());
    }
}
