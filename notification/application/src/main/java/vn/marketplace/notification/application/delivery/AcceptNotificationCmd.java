package vn.marketplace.notification.application.delivery;

import java.util.Map;

/**
 * Accept command. {@code variables} is the PLAINTEXT template data as received; the use-case encrypts
 * sensitive values (L4) before persisting. {@code idempotencyKey} dedups (REST: caller key; event:
 * {@code {eventType}:{eventId}:{recipientRole}}).
 */
public record AcceptNotificationCmd(String idempotencyKey,
                                    String userId,
                                    String channel,
                                    String templateId,
                                    Map<String, String> variables,
                                    String priority,
                                    String sourceEventType,
                                    String sourceEventId) {
}
