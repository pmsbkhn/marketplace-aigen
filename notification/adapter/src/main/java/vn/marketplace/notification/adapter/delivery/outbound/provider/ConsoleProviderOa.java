package vn.marketplace.notification.adapter.delivery.outbound.provider;

import java.util.Map;

import org.springframework.stereotype.Component;

import vn.marketplace.notification.application.delivery.NotificationProviderPort;
import vn.marketplace.notification.domain.delivery.Channel;

/**
 * Email/SMS/push provider adapter. <strong>Stand-in</strong>: resolves the channel to a provider id and
 * "delivers" successfully (no external call) on the standalone profile. Production integrates SES /
 * Twilio / FCM with provider-side idempotency keys and transient/permanent error classification.
 */
@Component
public class ConsoleProviderOa implements NotificationProviderPort {

    @Override
    public String send(Channel channel, String userId, String templateId, Map<String, String> variables) {
        return switch (channel) {
            case EMAIL, AUTO -> "SES";
            case SMS -> "TWILIO";
            case PUSH -> "FCM";
        };
    }
}
