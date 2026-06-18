# ==============================================================================
# CONTEXT: Notification  —  C4 Level 3 (Component)
# As-built: JAVA/Spring Boot. 1 app DUY NHẤT (không tách Consumer/Relay/Scheduler/
# Dispatcher). KHÔNG Redis/SQS/DLQ/template engine/scheduler. Provider/Encryption/
# Preferences đều STAND-IN. Hiện chỉ consume PaymentReceived.
# ==============================================================================

group "Notification Context" {

    notifDb = container "Notification Database" "Notifications + delivery_attempts (biến nội dung đã mã hoá) + outbox. Idempotency = UNIQUE idempotency_key." "PostgreSQL" "Database,Sensitive"

    notifApi = container "Notification Service" "Accept/Dispatch/Query notification; 1 Kafka consumer in-process (PaymentReceived). Đơn tiến trình — không queue/relay/scheduler/worker tách riêng." "Spring Boot 4 / Java 21 / msfw" {

        # --- A. INBOUND ---
        notifController           = component "NotificationController" "REST: POST /v1/notifications (202), GET /v1/notifications/{id}" "Spring MVC" "Ingress"
        internalDispatchController= component "InternalDispatchController" "REST: POST /internal/notifications/{id}/dispatch (stand-in cho worker, S2S)" "Spring MVC" "InternalApi"
        notifEventsFacade         = component "NotificationEventsFacade" "Kafka consumer: PaymentReceived -> notification cho merchant" "msfw consumer"

        # --- B. APPLICATION ---
        acceptUc   = component "AcceptNotificationUc" "Accept idempotent; mã hoá biến nội dung trước khi lưu" "Use case"
        dispatchUc = component "DispatchNotificationUc" "Render + gửi; ghi delivery attempt" "Use case"
        getUc      = component "GetNotificationUc" "Đọc trạng thái" "Use case"

        # --- C. DOMAIN ---
        notifDomain = component "Notification (Aggregate)" "Máy trạng thái ACCEPTED -> RENDERED -> SENT | SUPPRESSED | FAILED; lịch sử attempt append-only" "Domain model"

        # --- D. OUTBOUND ---
        notifOa      = component "NotificationOa" "Repository qua AbstractMementoJpaOa" "Spring Data JPA"
        notifOutbox  = component "Outbox Publisher" "Drain outbox" "msfw outbox"
        encryptionOa = component "Base64EncryptionOa" "Mã hoá field-level (stand-in; AES-256/KMS ở prod)" "Stand-in" "Sensitive,Standin"
        prefsClient  = component "PreferencesClientOa" "Kiểm tra opt-out (stand-in; trả false)" "Stand-in" "Standin"
        providerOa   = component "ConsoleProviderOa" "Channel provider stand-in (SES/Twilio/FCM ở prod)" "Stand-in" "Standin"

        # --- Internal wiring ---
        notifController            -> acceptUc
        notifController            -> getUc
        internalDispatchController -> dispatchUc
        notifEventsFacade          -> acceptUc
        notifEventsFacade          -> dispatchUc

        acceptUc   -> notifDomain
        dispatchUc -> notifDomain

        acceptUc   -> encryptionOa
        dispatchUc -> prefsClient
        dispatchUc -> providerOa

        acceptUc   -> notifOa
        dispatchUc -> notifOa
        getUc      -> notifOa

        notifOa     -> notifDb "Đọc/ghi (atomic + outbox)" "JDBC/TLS"
        notifOutbox -> notifDb "Poll outbox" "JDBC/TLS"
    }
}
