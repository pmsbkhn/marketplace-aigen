package vn.marketplace.payment.application.payment;

import java.util.List;
import java.util.UUID;

import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.payment.domain.payment.PayoutId;
import vn.marketplace.payment.domain.payment.SettlementId;
import vn.marketplace.payment.domain.payment.management.CommissionPolicy;
import vn.marketplace.payment.domain.payment.management.EscrowHold;
import vn.marketplace.payment.domain.payment.management.Payment;
import vn.marketplace.payment.domain.payment.management.Settlement;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * Settles one completed order:
 * <ol>
 *   <li>idempotency — an existing settlement for {@code orderId} is returned as-is;</li>
 *   <li>release the escrow hold via the aggregate (PAID guard inside, TC-PAY-03);</li>
 *   <li>cross-check the order's item total against the hold (anti drift, defense-in-depth);</li>
 *   <li>commission via {@link CommissionPolicy} (standard 2%);</li>
 *   <li>write the WORM document, attach its URI, then complete (no doc → no completion, TC-PAY-04).</li>
 * </ol>
 * State-writing → {@link EventPublishHandler} (outbox proxy; payout submission and its
 * {@code PayoutCompleted} happen in {@link ProcessPayoutUc}).
 */
public class ProcessSettlementUc implements ProcessSettlement {

    private final Repository<Payment> paymentRepository;
    private final Repository<Settlement> settlementRepository;
    private final SettlementDocWriter docWriter;
    private final CommissionPolicy commissionPolicy;

    public ProcessSettlementUc(Repository<Payment> paymentRepository,
                               Repository<Settlement> settlementRepository,
                               SettlementDocWriter docWriter,
                               CommissionPolicy commissionPolicy) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.docWriter = docWriter;
        this.commissionPolicy = commissionPolicy;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public SettlementView execute(ProcessSettlementCmd cmd) {
        List<Settlement> existing = settlementRepository.findBy(SettlementCriteria.byOrderId(cmd.orderId()));
        if (!existing.isEmpty()) {
            return SettlementView.from(existing.get(0)); // idempotent — already settled
        }

        Payment payment = paymentRepository.findBy(PaymentCriteria.byOrderId(cmd.orderId())).stream()
                .findFirst()
                .orElseThrow(() -> new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "No payment holds escrow for order " + cmd.orderId()));

        EscrowHold hold = payment.releaseForOrder(cmd.orderId()); // PAID guard inside (TC-PAY-03)

        crossCheckOrderTotal(cmd, hold);

        // Ids derived from the orderId: a retry after a partial failure regenerates the identical
        // settlement document, so the write-once WORM store accepts it instead of denying a "new" doc.
        Settlement settlement = Settlement.start(deterministicSettlementId(cmd.orderId()),
                cmd.orderId(), hold.merchantId(), hold.amount(), commissionPolicy,
                deterministicPayoutId(cmd.orderId()), cmd.merchantBankAccount());

        String docUri = docWriter.writeOnce(settlementDocKey(settlement), settlementDocContent(settlement));
        settlement.attachDocument(docUri);
        settlement.complete();

        paymentRepository.save(payment);
        settlementRepository.save(settlement);
        return SettlementView.from(settlement);
    }

    /** The escrowed amount must equal Σ priceSnapshot×qty of the completed order — veto any drift. */
    private void crossCheckOrderTotal(ProcessSettlementCmd cmd, EscrowHold hold) {
        if (cmd.items() == null || cmd.items().isEmpty()) {
            return; // event without line detail — the hold amount is authoritative
        }
        long orderTotal = cmd.items().stream()
                .mapToLong(i -> i.priceSnapshot() * i.quantity())
                .sum();
        if (orderTotal != hold.amount().amount()) {
            throw new PaymentDomainException(PaymentErrorCode.AMOUNT_MISMATCH,
                    "Order " + cmd.orderId() + " total " + orderTotal
                            + " does not match escrowed " + hold.amount().amount());
        }
    }

    private SettlementId deterministicSettlementId(String orderId) {
        return new SettlementId(UUID.nameUUIDFromBytes(("settlement:" + orderId).getBytes()).toString());
    }

    private PayoutId deterministicPayoutId(String orderId) {
        return new PayoutId(UUID.nameUUIDFromBytes(("payout:" + orderId).getBytes()).toString());
    }

    private String settlementDocKey(Settlement s) {
        return "settlements/" + s.orderId() + ".json";
    }

    /** Deterministic reconciliation document — same settlement → same bytes (safe WORM retry). */
    private String settlementDocContent(Settlement s) {
        return "{"
                + "\"settlementId\":\"" + s.id().value() + "\","
                + "\"orderId\":\"" + s.orderId() + "\","
                + "\"merchantId\":\"" + s.merchantId().value() + "\","
                + "\"gross\":" + s.grossAmount().amount() + ","
                + "\"commission\":" + s.commission().amount() + ","
                + "\"net\":" + s.netAmount().amount() + ","
                + "\"currency\":\"" + s.grossAmount().currency().name() + "\""
                + "}";
    }
}
