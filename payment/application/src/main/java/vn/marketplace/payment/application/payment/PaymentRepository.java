package vn.marketplace.payment.application.payment;

import java.util.Optional;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import vn.marketplace.payment.domain.payment.management.Payment;

/**
 * Persistence port for {@link Payment} with one lookup the generic Criteria DSL cannot express:
 * locating the payment that owns an order's escrow hold requires a join into the {@code holds}
 * child collection, which the msfw Criteria → Specification translator does not support.
 */
public interface PaymentRepository extends Repository<Payment> {

    /** The payment whose escrow {@code holds} contain an allocation for {@code orderId}. */
    Optional<Payment> findByHoldOrderId(String orderId);
}
