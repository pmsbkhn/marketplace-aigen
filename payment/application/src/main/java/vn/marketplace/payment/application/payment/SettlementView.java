package vn.marketplace.payment.application.payment;

import vn.marketplace.payment.domain.payment.management.Settlement;

/** Read model of one settlement (bank account intentionally absent — L4 stays in the aggregate). */
public record SettlementView(String settlementId,
                             String orderId,
                             String merchantId,
                             long grossAmount,
                             long commission,
                             long netAmount,
                             String currency,
                             String status,
                             String docUri,
                             String payoutId,
                             String payoutStatus) {

    public static SettlementView from(Settlement s) {
        return new SettlementView(s.id().value(), s.orderId(), s.merchantId().value(),
                s.grossAmount().amount(), s.commission().amount(), s.netAmount().amount(),
                s.grossAmount().currency().name(), s.status().name(), s.docUri(),
                s.payout().id().value(), s.payout().status().name());
    }
}
