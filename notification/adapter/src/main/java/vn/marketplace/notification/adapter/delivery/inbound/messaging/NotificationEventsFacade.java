package vn.marketplace.notification.adapter.delivery.inbound.messaging;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.vsf.ptnt.msfw.event.causation.EventCausation;
import vn.marketplace.notification.application.delivery.AcceptNotification;
import vn.marketplace.notification.application.delivery.AcceptNotificationCmd;
import vn.marketplace.notification.application.delivery.DispatchNotification;
import vn.marketplace.notification.application.delivery.DispatchNotificationCmd;
import vn.marketplace.notification.application.delivery.NotificationIdView;

/**
 * Event-subscriber boundary (tech spec: {@code event-subscriber}) — maps marketplace events to
 * notification requests per the fixed event→template mapping (tech spec §4.1.2). Idempotency key
 * = {@code {eventType}:{eventId}:{recipientRole}:{orderId}}; {@code eventId} comes from
 * {@link EventCausation}.
 *
 * <p>{@code PaymentReceived} → one merchant notification per allocation
 * ({@code payment_received_merchant}). The buyer notification of the mapping table needs
 * {@code buyerId}, which the event does not carry yet — publisher-side enrichment is a follow-up.</p>
 */
@Service
public class NotificationEventsFacade {

    static final String PAYMENT_RECEIVED_MERCHANT_TEMPLATE = "payment_received_merchant";

    private final AcceptNotification acceptNotification;
    private final DispatchNotification dispatchNotification;

    public NotificationEventsFacade(AcceptNotification acceptNotification,
                                    DispatchNotification dispatchNotification) {
        this.acceptNotification = acceptNotification;
        this.dispatchNotification = dispatchNotification;
    }

    @Transactional
    public void onPaymentReceived(PaymentReceivedData data) {
        String eventId = consumedEventId();
        List<PaymentReceivedData.Allocation> allocations =
                data.allocations() == null ? List.of() : data.allocations();
        for (PaymentReceivedData.Allocation allocation : allocations) {
            NotificationIdView accepted = acceptNotification.execute(new AcceptNotificationCmd(
                    "PaymentReceived:%s:MERCHANT:%s".formatted(eventId, allocation.orderId()),
                    allocation.merchantId(),
                    "AUTO",
                    PAYMENT_RECEIVED_MERCHANT_TEMPLATE,
                    Map.of("orderId", allocation.orderId(),
                            "amount", String.valueOf(allocation.amount()),
                            "paymentId", data.paymentId()),
                    "TRANSACTIONAL",
                    "PaymentReceived",
                    eventId));
            dispatchNotification.execute(new DispatchNotificationCmd(accepted.notificationId()));
        }
    }

    private static String consumedEventId() {
        return EventCausation.current().map(EventCausation.Causation::causationId).orElse(null);
    }
}
