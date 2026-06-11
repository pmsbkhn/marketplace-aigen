package vn.marketplace.payment.application.payment;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.msfw.domain.core.Repository;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;
import tech.vsf.ptnt.msfw.outbox.store.JsonEventStoreProcessor;
import vn.marketplace.payment.domain.payment.PayoutStatus;
import vn.marketplace.payment.domain.payment.management.Payout;
import vn.marketplace.payment.domain.payment.management.Settlement;
import vn.marketplace.payment.domain.shared.PaymentDomainException;
import vn.marketplace.payment.domain.shared.PaymentErrorCode;

/**
 * Submits the payout of a settlement to the merchant bank. Already-SUBMITTED → silent no-op (no
 * duplicate bank instruction, TC-PAY-06). The bank call happens <em>before</em> the state flip and
 * both share the use-case transaction: a bank failure rolls everything back and the retry is safe.
 * State-writing → {@link EventPublishHandler}: {@code PayoutCompleted} drains to the outbox.
 */
public class ProcessPayoutUc implements ProcessPayout {

    private final Repository<Settlement> settlementRepository;
    private final BankPort bank;

    public ProcessPayoutUc(Repository<Settlement> settlementRepository, BankPort bank) {
        this.settlementRepository = settlementRepository;
        this.bank = bank;
    }

    @Override
    @EventPublishHandler(eventProcessors = {JsonEventStoreProcessor.class})
    public SettlementView execute(ProcessPayoutCmd cmd) {
        Settlement settlement = settlementRepository.findBy(Criteria.where("orderId").eq(cmd.orderId())).stream()
                .findFirst()
                .orElseThrow(() -> new PaymentDomainException(PaymentErrorCode.SETTLEMENT_NOT_FOUND,
                        "No settlement for order " + cmd.orderId()));

        Payout payout = settlement.payout();
        if (payout.status() == PayoutStatus.SUBMITTED) {
            return SettlementView.from(settlement); // idempotent — never a second bank instruction
        }

        bank.submitPayout(payout.id().value(), payout.merchantId().value(), payout.bankAccount(),
                payout.amount().amount(), payout.amount().currency().name());
        settlement.submitPayout(); // publishes PayoutCompleted exactly once

        settlementRepository.save(settlement);
        return SettlementView.from(settlement);
    }
}
