package vn.marketplace.payment.application.payment;

import java.util.List;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.management.Payment;

/**
 * Inits the whole-cart escrow. Idempotent by {@code orderRef}: a duplicate request returns the
 * existing payment URL without registering a second gateway transaction. Allocation-sum and
 * positive-amount invariants are enforced by the {@link Payment} aggregate. State-writing →
 * {@link EventPublishHandler} (no event on init, but the outbox proxy wraps every state-changing
 * use-case — fitness rule).
 */
public class InitEscrowUc implements InitEscrow {

    /** Escrow/payment session lifetime — must stay aligned with checkout.reserve_ttl_min. */
    private static final int EXPIRES_MINUTES = 15;

    private final Repository<Payment> paymentRepository;
    private final PaymentGatewayPort gateway;

    public InitEscrowUc(Repository<Payment> paymentRepository, PaymentGatewayPort gateway) {
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public InitEscrowResult execute(InitEscrowCmd cmd) {
        List<Payment> existing = paymentRepository.findBy(PaymentCriteria.byOrderRef(cmd.orderRef()));
        if (!existing.isEmpty()) {
            Payment payment = existing.get(0);
            return new InitEscrowResult(payment.id().value(), payment.paymentUrl(),
                    payment.createdAt().plusMinutes(EXPIRES_MINUTES));
        }

        List<Payment.NewAllocation> allocations = cmd.allocations() == null ? List.of()
                : cmd.allocations().stream()
                        .map(a -> new Payment.NewAllocation(UUID.randomUUID().toString(),
                                a.orderId(), a.merchantId(), a.amount()))
                        .toList();

        Payment payment = Payment.initEscrow(new PaymentId(UUID.randomUUID().toString()), cmd.orderRef(),
                Money.of(cmd.amount(), Currency.of(cmd.currency())), allocations);

        String paymentUrl = gateway.createTransaction(payment.id().value(), cmd.orderRef(),
                cmd.amount(), cmd.currency());
        payment.attachPaymentUrl(paymentUrl);

        paymentRepository.save(payment);
        return new InitEscrowResult(payment.id().value(), paymentUrl,
                payment.createdAt().plusMinutes(EXPIRES_MINUTES));
    }
}
