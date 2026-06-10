package vn.marketplace.notification.adapter.delivery.dto;

import java.util.Map;

/**
 * Body for POST /v1/notifications. {@code variables} is plaintext as received; the use-case encrypts
 * sensitive values before persisting. {@code channel} ∈ EMAIL|SMS|PUSH|AUTO, {@code priority} ∈
 * TRANSACTIONAL|BULK.
 */
public record AcceptNotificationRequest(String idempotencyKey,
                                        String userId,
                                        String channel,
                                        String templateId,
                                        Map<String, String> variables,
                                        String priority) {
}
