package vn.marketplace.payment.adapter.escrow;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import vn.marketplace.payment.application.escrow.EscrowView;
import vn.marketplace.payment.application.escrow.EscrowViewStore;

/** JPA adapter for the escrow read model — upsert by escrow id into {@code escrow_view}. */
@Repository
public class JpaEscrowViewStore implements EscrowViewStore {

    private final EscrowViewJpaRepository repository;

    public JpaEscrowViewStore(EscrowViewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<EscrowView> find(String escrowId) {
        return repository.findById(escrowId)
                .map(e -> new EscrowView(e.getEscrowId(), e.isOpen(), e.getHeld(), e.getReleased()));
    }

    @Override
    public void save(EscrowView view) {
        repository.save(new EscrowViewEntity(view.escrowId(), view.open(), view.held(), view.released()));
    }
}
