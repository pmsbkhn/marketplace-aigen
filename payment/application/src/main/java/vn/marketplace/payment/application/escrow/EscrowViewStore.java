package vn.marketplace.payment.application.escrow;

import java.util.Optional;

/** Outbound port for the escrow read model (query side). Implemented by a JPA adapter. */
public interface EscrowViewStore {

    Optional<EscrowView> find(String escrowId);

    void save(EscrowView view);
}
