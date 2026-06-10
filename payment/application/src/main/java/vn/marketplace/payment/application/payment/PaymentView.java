package vn.marketplace.payment.application.payment;

import java.util.List;

import vn.marketplace.payment.domain.payment.management.Payment;

/** Read model of one payment + its escrow allocations (no L4 data). */
public record PaymentView(String paymentId,
                          String orderRef,
                          String status,
                          long amount,
                          String currency,
                          String paymentUrl,
                          String gatewayTxnId,
                          List<HoldView> holds) {

    public record HoldView(String orderId, String merchantId, long amount, String status) {
    }

    public static PaymentView from(Payment p) {
        List<HoldView> holds = p.holds().stream()
                .map(h -> new HoldView(h.orderId(), h.merchantId().value(), h.amount().amount(),
                        h.status().name()))
                .toList();
        return new PaymentView(p.id().value(), p.orderRef(), p.status().name(), p.amount().amount(),
                p.amount().currency().name(), p.paymentUrl(), p.gatewayTxnId(), holds);
    }
}
