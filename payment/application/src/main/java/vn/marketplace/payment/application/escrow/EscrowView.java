package vn.marketplace.payment.application.escrow;

/** CQRS read model for escrow — a denormalized view built by projecting the escrow event stream. */
public record EscrowView(String escrowId, boolean open, long held, long released) {
}
