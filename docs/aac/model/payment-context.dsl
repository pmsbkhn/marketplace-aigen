# ==============================================================================
# CONTEXT: Payment (Escrow)  —  C4 Level 3 (Component)
# As-built (msfw 1.0.0): Payment & Settlement state-stored (memento); EscrowLedger
# EVENT-SOURCED (event store + snapshot) + read model EscrowView (CQRS).
# Webhook gateway (HMAC) là REST thật; gRPC InitEscrow = REST /internal/* stand-in.
# ==============================================================================

group "Payment Context" {

    paymentDb = container "Payment Database" "Payment & Settlement (memento), event store + snapshot của EscrowLedger, read model escrow_view, outbox." "PostgreSQL" "Database,Sensitive"

    paymentDocStore = container "Settlement Docs Store" "Chứng từ đối soát ghi-một-lần (S3 Object Lock/WORM ở prod cloud; local FS ở standalone)." "AWS S3 / FS" "Database,Sensitive"

    paymentApi = container "Payment Service" "Init escrow, webhook gateway, đối soát + payout. Escrow ledger event-sourced + CQRS read model." "Spring Boot 4 / Java 21 / msfw 1.0.0" {

        # --- A. INBOUND ---
        webhookController         = component "WebhookController" "REST: POST /v1/payments/webhook (xác minh HMAC + chống replay)" "Spring MVC" "Ingress"
        internalPaymentController = component "InternalPaymentController" "REST: POST /internal/payments/escrow, GET /{orderRef} — stand-in cho gRPC InitEscrow (S2S)" "Spring MVC" "InternalApi"
        orderEventsFacade         = component "OrderEventsFacade" "Kafka consumer: OrderCompleted -> đối soát + payout" "msfw consumer"

        # --- B. APPLICATION ---
        initEscrowUc        = component "InitEscrowUc" "Tạo Payment (PENDING), gọi gateway lấy payment URL" "Use case"
        handleWebhookUc     = component "HandleWebhookUc" "PENDING -> PAID/FAILED; amount cross-check; publish PaymentReceived/PaymentFailed" "Use case"
        processSettlementUc = component "ProcessSettlementUc" "Giải phóng escrow, tính hoa hồng (2%), ghi chứng từ WORM" "Use case"
        processPayoutUc     = component "ProcessPayoutUc" "Lên lệnh payout cho bank (idempotent); publish PayoutCompleted" "Use case"
        getPaymentUc        = component "GetPaymentUc" "Đọc payment theo orderRef" "Use case"
        escrowLedgerService = component "EscrowLedgerService + EscrowProjector" "Write model event-sourced + chiếu in-process sang EscrowView (CQRS)" "Use case / projector"

        # --- C. DOMAIN ---
        paymentDomain = component "Payment / Settlement / EscrowLedger / CommissionPolicy" "Payment & Settlement state-stored; EscrowLedger event-sourced (EscrowOpened/FundsHeld/FundsReleased); hoa hồng 2%" "Domain model"

        # --- D. OUTBOUND ---
        paymentOa     = component "PaymentOa / SettlementOa" "Repository qua AbstractMementoJpaOa; UNIQUE gateway_txn_id (idempotency)" "Spring Data JPA"
        escrowStore   = component "Escrow Event Store + View Store" "msfw JPA event store + snapshot; bảng escrow_view" "msfw event-sourcing"
        paymentOutbox = component "Outbox Publisher" "Drain outbox -> Kafka (PaymentReceived/Failed, PayoutCompleted)" "msfw outbox"
        gatewayClient = component "GatewayClientOa" "HTTPS + HMAC ra cổng thanh toán" "HTTP client" "Sensitive"
        bankClient    = component "BankClientOa" "HTTPS ra ngân hàng để payout" "HTTP client" "Sensitive"
        docWriter     = component "SettlementDocWriterOa" "Ghi chứng từ một lần (S3 WORM / FS)" "AWS SDK / FS"

        # --- Internal wiring ---
        webhookController         -> handleWebhookUc
        internalPaymentController -> initEscrowUc
        internalPaymentController -> getPaymentUc
        orderEventsFacade         -> processSettlementUc
        orderEventsFacade         -> processPayoutUc

        initEscrowUc        -> paymentDomain
        handleWebhookUc     -> paymentDomain
        processSettlementUc -> paymentDomain
        processPayoutUc     -> paymentDomain

        initEscrowUc        -> escrowLedgerService
        processSettlementUc -> escrowLedgerService
        escrowLedgerService -> escrowStore

        initEscrowUc        -> gatewayClient
        initEscrowUc        -> paymentOa
        handleWebhookUc     -> paymentOa
        processSettlementUc -> docWriter
        processSettlementUc -> paymentOa
        processPayoutUc     -> bankClient
        processPayoutUc     -> paymentOa
        getPaymentUc        -> paymentOa

        paymentOa     -> paymentDb "Đọc/ghi" "JDBC/TLS"
        escrowStore   -> paymentDb "Append events / đọc view" "JDBC/TLS"
        paymentOutbox -> paymentDb "Poll outbox" "JDBC/TLS"
        docWriter     -> paymentDocStore "Ghi-một-lần" "HTTPS/IAM"
    }
}
