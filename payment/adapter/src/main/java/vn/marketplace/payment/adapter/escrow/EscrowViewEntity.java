package vn.marketplace.payment.adapter.escrow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA row for the escrow CQRS read model ({@code escrow_view}) in the Payment database. */
@Entity
@Table(name = "escrow_view")
public class EscrowViewEntity {

    @Id
    @Column(name = "escrow_id", length = 256)
    private String escrowId;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "held", nullable = false)
    private long held;

    @Column(name = "released", nullable = false)
    private long released;

    protected EscrowViewEntity() {
    }

    public EscrowViewEntity(String escrowId, boolean open, long held, long released) {
        this.escrowId = escrowId;
        this.open = open;
        this.held = held;
        this.released = released;
    }

    public String getEscrowId() {
        return escrowId;
    }

    public boolean isOpen() {
        return open;
    }

    public long getHeld() {
        return held;
    }

    public long getReleased() {
        return released;
    }
}
