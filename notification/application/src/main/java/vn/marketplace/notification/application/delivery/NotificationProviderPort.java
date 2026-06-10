package vn.marketplace.notification.application.delivery;

import java.util.Map;

import vn.marketplace.notification.domain.delivery.Channel;

/**
 * Outbound port to the email/SMS/push providers (SES / Twilio / FCM). Implementations attach a
 * provider-side idempotency key and translate the provider response. Throws
 * {@link ProviderException} on a delivery failure (transient or permanent).
 */
public interface NotificationProviderPort {

    /** Send the rendered message; returns the provider id used (e.g. "SES", "TWILIO"). */
    String send(Channel channel, String userId, String templateId, Map<String, String> variables);

    /** A provider delivery failure, classified transient vs permanent. */
    class ProviderException extends RuntimeException {
        private final boolean permanent;
        private final String provider;

        public ProviderException(String provider, boolean permanent, String message) {
            super(message);
            this.provider = provider;
            this.permanent = permanent;
        }

        public boolean isPermanent() {
            return permanent;
        }

        public String provider() {
            return provider;
        }
    }
}
