package vn.marketplace.notification.domain.delivery.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import vn.marketplace.notification.domain.delivery.Channel;
import vn.marketplace.notification.domain.delivery.NotificationId;
import vn.marketplace.notification.domain.delivery.NotificationStatus;
import vn.marketplace.notification.domain.delivery.Priority;
import vn.marketplace.notification.domain.shared.NotificationDomainException;
import vn.marketplace.notification.domain.shared.NotificationErrorCode;

/**
 * Notification aggregate root — one outbound notification through its lifecycle.
 *
 * <p>State machine (one-directional; terminal states immutable):
 * <pre>
 *   ACCEPTED --markRendered--> RENDERED --recordSent--> SENT
 *   ACCEPTED | RENDERED --suppress--> SUPPRESSED        (opt-out)
 *   ACCEPTED | RENDERED --fail--> FAILED                (provider exhausted)
 * </pre>
 * Any out-of-cycle transition (e.g. ACCEPTED→SENT skipping RENDERED) throws
 * {@code INVALID_TRANSITION}. The {@code variables} map holds <strong>already-encrypted</strong>
 * values (the application layer ciphers sensitive data such as OTP before construction — L4), so the
 * aggregate never carries plaintext. {@code idempotencyKey} is immutable.
 */
public class Notification extends Aggregate<NotificationId> {

    private final String idempotencyKey;
    private final String userId;
    private final Channel channel;
    private final String templateId;
    private final Map<String, String> variables; // ciphertext values (encrypted upstream)
    private final Priority priority;
    private NotificationStatus status;
    private final String sourceEventType;
    private final String sourceEventId;
    private final List<DeliveryAttempt> attempts;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Notification(NotificationId id, String idempotencyKey, String userId, Channel channel,
                         String templateId, Map<String, String> variables, Priority priority,
                         String sourceEventType, String sourceEventId) {
        if (templateId == null || templateId.isBlank()) {
            throw new NotificationDomainException(NotificationErrorCode.MISSING_TEMPLATE);
        }
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.channel = channel;
        this.templateId = templateId;
        this.variables = new LinkedHashMap<>(variables == null ? Map.of() : variables);
        this.priority = priority;
        this.status = NotificationStatus.ACCEPTED;
        this.sourceEventType = sourceEventType;
        this.sourceEventId = sourceEventId;
        this.attempts = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    private Notification(Memento m, List<DeliveryAttempt> attempts) {
        this.id = new NotificationId(m.notificationId());
        this.set_id(m._id());
        this.idempotencyKey = m.idempotencyKey();
        this.userId = m.userId();
        this.channel = Channel.valueOf(m.channel());
        this.templateId = m.templateId();
        this.variables = new LinkedHashMap<>(m.variables());
        this.priority = Priority.valueOf(m.priority());
        this.status = NotificationStatus.valueOf(m.status());
        this.sourceEventType = m.sourceEventType();
        this.sourceEventId = m.sourceEventId();
        this.attempts = new ArrayList<>(attempts);
        this.createdAt = m.createdAt();
        this.updatedAt = m.updatedAt();
    }

    /** Ingest a new notification (status ACCEPTED). {@code variables} must already be encrypted. */
    public static Notification accept(NotificationId id, String idempotencyKey, String userId, Channel channel,
                                      String templateId, Map<String, String> variables, Priority priority,
                                      String sourceEventType, String sourceEventId) {
        return new Notification(id, idempotencyKey, userId, channel, templateId, variables, priority,
                sourceEventType, sourceEventId);
    }

    // ---- verb methods (state machine) ----

    /** ACCEPTED → RENDERED. */
    public void markRendered() {
        requireStatus(NotificationStatus.ACCEPTED);
        this.status = NotificationStatus.RENDERED;
        touch();
    }

    /** RENDERED → SENT (records a successful delivery attempt). */
    public void recordSent(String provider) {
        requireStatus(NotificationStatus.RENDERED);
        this.attempts.add(new DeliveryAttempt(attempts.size() + 1, provider, "SUCCESS", LocalDateTime.now()));
        this.status = NotificationStatus.SENT;
        touch();
    }

    /** ACCEPTED | RENDERED → SUPPRESSED (opt-out; provider is NOT called). */
    public void suppress(String reason) {
        requireActive();
        this.status = NotificationStatus.SUPPRESSED;
        touch();
    }

    /** ACCEPTED | RENDERED → FAILED (records the failed attempt). */
    public void fail(String provider, String result) {
        requireActive();
        this.attempts.add(new DeliveryAttempt(attempts.size() + 1, provider, result, LocalDateTime.now()));
        this.status = NotificationStatus.FAILED;
        touch();
    }

    private void requireStatus(NotificationStatus expected) {
        if (this.status != expected) {
            throw new NotificationDomainException(NotificationErrorCode.INVALID_TRANSITION,
                    "Expected " + expected + " but was " + this.status);
        }
    }

    private void requireActive() {
        if (this.status != NotificationStatus.ACCEPTED && this.status != NotificationStatus.RENDERED) {
            throw new NotificationDomainException(NotificationErrorCode.INVALID_TRANSITION,
                    "Notification is already terminal: " + this.status);
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- queries / accessors ----

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String userId() {
        return userId;
    }

    public Channel channel() {
        return channel;
    }

    public String templateId() {
        return templateId;
    }

    public Map<String, String> variables() {
        return Map.copyOf(variables);
    }

    public Priority priority() {
        return priority;
    }

    public NotificationStatus status() {
        return status;
    }

    public String sourceEventType() {
        return sourceEventType;
    }

    public String sourceEventId() {
        return sourceEventId;
    }

    public List<DeliveryAttempt> attempts() {
        return List.copyOf(attempts);
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    // ---- Memento (persistence reconstruction) ----

    public static Notification fromMemento(Memento m) {
        List<DeliveryAttempt> attempts = m.attempts().stream().map(DeliveryAttempt::fromMemento).toList();
        return new Notification(m, attempts);
    }

    public Memento toMemento() {
        List<AttemptMemento> attemptMementos = new ArrayList<>();
        for (DeliveryAttempt a : attempts) {
            attemptMementos.add(new AttemptMemento(a.attemptNo(), a.provider(), a.result(), a.attemptedAt()));
        }
        return new Memento(_id(), id.value(), idempotencyKey, userId, channel.name(), templateId,
                new LinkedHashMap<>(variables), priority.name(), status.name(), sourceEventType, sourceEventId,
                createdAt, updatedAt, attemptMementos);
    }

    public record Memento(Long _id,
                          String notificationId,
                          String idempotencyKey,
                          String userId,
                          String channel,
                          String templateId,
                          Map<String, String> variables,
                          String priority,
                          String status,
                          String sourceEventType,
                          String sourceEventId,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          List<AttemptMemento> attempts) {
    }

    public record AttemptMemento(int attemptNo, String provider, String result, LocalDateTime attemptedAt) {
    }
}
