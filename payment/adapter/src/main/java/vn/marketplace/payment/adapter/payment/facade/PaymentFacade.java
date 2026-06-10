package vn.marketplace.payment.adapter.payment.facade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.marketplace.payment.adapter.payment.dto.OrderCompletedEvent;
import vn.marketplace.payment.adapter.payment.outbound.gateway.MerchantBankDirectory;
import vn.marketplace.payment.application.payment.GetPayment;
import vn.marketplace.payment.application.payment.GetPaymentCmd;
import vn.marketplace.payment.application.payment.HandleWebhook;
import vn.marketplace.payment.application.payment.HandleWebhookCmd;
import vn.marketplace.payment.application.payment.InitEscrow;
import vn.marketplace.payment.application.payment.InitEscrowCmd;
import vn.marketplace.payment.application.payment.InitEscrowResult;
import vn.marketplace.payment.application.payment.PaymentView;
import vn.marketplace.payment.application.payment.ProcessPayout;
import vn.marketplace.payment.application.payment.ProcessPayoutCmd;
import vn.marketplace.payment.application.payment.ProcessSettlement;
import vn.marketplace.payment.application.payment.ProcessSettlementCmd;
import vn.marketplace.payment.application.payment.SettlementView;

/** Transactional application boundary for the payment use-cases. */
@Service
public class PaymentFacade {

    private final InitEscrow initEscrow;
    private final HandleWebhook handleWebhook;
    private final ProcessSettlement processSettlement;
    private final ProcessPayout processPayout;
    private final GetPayment getPayment;
    private final MerchantBankDirectory merchantBankDirectory;

    public PaymentFacade(InitEscrow initEscrow,
                         HandleWebhook handleWebhook,
                         ProcessSettlement processSettlement,
                         ProcessPayout processPayout,
                         GetPayment getPayment,
                         MerchantBankDirectory merchantBankDirectory) {
        this.initEscrow = initEscrow;
        this.handleWebhook = handleWebhook;
        this.processSettlement = processSettlement;
        this.processPayout = processPayout;
        this.getPayment = getPayment;
        this.merchantBankDirectory = merchantBankDirectory;
    }

    @Transactional
    public InitEscrowResult initEscrow(InitEscrowCmd cmd) {
        return initEscrow.execute(cmd);
    }

    @Transactional
    public void handleWebhook(HandleWebhookCmd cmd) {
        handleWebhook.execute(cmd);
    }

    /**
     * OrderCompleted → settle then pay out, one transaction: a payout failure rolls the settlement
     * back so the (idempotent) event redelivery retries both.
     */
    @Transactional
    public SettlementView settleAndPayout(OrderCompletedEvent event) {
        List<ProcessSettlementCmd.SettlementItemInput> items = event.items() == null ? List.of()
                : event.items().stream()
                        .map(i -> new ProcessSettlementCmd.SettlementItemInput(i.sku(), i.priceSnapshot(), i.qty()))
                        .toList();
        processSettlement.execute(new ProcessSettlementCmd(event.orderId(), event.merchantId(), items,
                merchantBankDirectory.bankAccountFor(event.merchantId())));
        return processPayout.execute(new ProcessPayoutCmd(event.orderId()));
    }

    @Transactional(readOnly = true)
    public PaymentView getByOrderRef(String orderRef) {
        return getPayment.execute(new GetPaymentCmd(orderRef));
    }
}
