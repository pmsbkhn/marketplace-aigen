# ==============================================================================
# MERGED / SELF-CONTAINED workspace — dán toàn bộ vào https://playground.structurizr.com
# Sinh ra từ workspace.dsl + model/*.dsl + views/*.dsl + styles.dsl (đã inline).
# Đã bỏ !include / !docs / !adrs (playground không có filesystem).
# Ranh giới Kiến trúc <-> Tech spec được giữ bằng các block "group" và mốc comment.
# NGUỒN SỰ THẬT vẫn là các file rời; file này là bản phái sinh để xem online.
# ==============================================================================

# ==============================================================================
# WORKSPACE: E-commerce Marketplace (as-built)
# File master Structurizr DSL — điểm vào duy nhất. Lắp ráp:
#   model/   -> các bounded context + hạ tầng + deployment (nguồn sự thật)
#   views/   -> các view nhóm theo stakeholder (ISO/IEC/IEEE 42010)
#   styles.dsl
#
# NGUYÊN TẮC: chỉ mô hình hoá những gì ĐÃ có trong source code & deploy/.
# Phần SAD chưa hiện thực được ghi chú trong description / ADR, KHÔNG tạo box giả.
# Xem README.md để biết toàn cảnh các tạp phẩm AaC của dự án.
# ==============================================================================

workspace "E-commerce Marketplace" "Kiến trúc as-built của sàn TMĐT multi-merchant (6 service msfw)" {

    model {
        # Suy ra quan hệ cấp cha (container<->container) từ quan hệ cấp component.
        # Cần cho dynamic view (saga) và deployment view có mũi tên.
        !impliedRelationships true

        # ----------------------------------------------------------------------
        # 1. ACTORS
        # ----------------------------------------------------------------------
        buyer    = person "Buyer" "Mua sắm, checkout, theo dõi & xác nhận đơn."
        merchant = person "Merchant" "Quản lý sản phẩm/giá, cập nhật kho, ship đơn."
        admin    = person "Platform Admin" "Kiểm duyệt sản phẩm."

        # ----------------------------------------------------------------------
        # 2. EXTERNAL SYSTEMS (chỉ những hệ thống thực sự được tích hợp trong code)
        # ----------------------------------------------------------------------
        pg       = softwareSystem "Payment Gateway" "Cổng thanh toán bên thứ ba (HMAC). Standalone dùng URL/redirect mô phỏng." "External"
        bank     = softwareSystem "Merchant Bank" "Ngân hàng nhận payout. Standalone chỉ log." "External"
        provider = softwareSystem "Notification Provider" "SES/Twilio/FCM ở prod. Hiện hiện thực = ConsoleProvider stand-in." "External"

        # ----------------------------------------------------------------------
        # 3. CORE SYSTEM & BOUNDED CONTEXTS
        # ----------------------------------------------------------------------
        marketplaceSystem = softwareSystem "Marketplace System" "Sàn TMĐT multi-merchant: 6 service Spring Boot/msfw (catalog, checkout, inventory, order, payment, notification)" {

        # ---------- inlined: model/shared-infra.dsl ----------
# ==============================================================================
# MODEL — Hạ tầng dùng chung ở tầng RUNTIME/ỨNG DỤNG (những gì code thực sự nối tới).
# Lưu ý: hạ tầng nền (Istio, Postgres, Redis, Elasticsearch, observability) được
# mô hình hoá ở deployment.dsl. Ở đây chỉ Kafka — thứ duy nhất application code
# hiện đang giao tiếp ở tầng tích hợp.
# ==============================================================================

group "Platform Infrastructure" {

    kafkaBus = container "Event Bus" "Trục sự kiện bất đồng bộ (CloudEvents/JSON). Producer = msfw transactional outbox; consumer = msfw consumption pipeline in-process. Hợp đồng wire: docs contracts/*.json." "Apache Kafka (Strimzi)" "MessageBus"
}
        # ---------- end: model/shared-infra.dsl ----------

        # ---------- inlined: model/catalog-context.dsl ----------
# ==============================================================================
# CONTEXT: Catalog  —  C4 Level 3 (Component)
# As-built: 1 Spring Boot app (publish-only). Search chạy trên DB (Elasticsearch
# đã provision ở infra nhưng app CHƯA wire — Phase C). Không S3 client, không cron.
# ==============================================================================

group "Catalog Context" {

    catalogDb = container "Catalog Database" "Nguồn sự thật: products, variants, SKUs + transactional outbox. Tìm kiếm ACTIVE phân trang chạy trực tiếp ở đây (DB thay cho Elasticsearch — chưa wire)." "PostgreSQL" "Database"

    catalogApi = container "Catalog Service" "CRUD sản phẩm, kiểm duyệt, tìm kiếm storefront, lấy giá nội bộ. Publish-only (outbox -> Kafka)." "Spring Boot 4 / Java 21 / msfw" {

        # --- A. INBOUND (REST) ---
        prodController   = component "ProductController" "REST: POST /v1/products, GET /v1/products/{id}, PUT /v1/products/{id}/skus/{skuCode}" "Spring MVC" "Ingress"
        adminController  = component "AdminProductController" "REST: POST /v1/admin/products/{id}/approve|reject" "Spring MVC" "Ingress"
        searchController = component "SearchController" "REST: GET /v1/search (q, category, brand, price range, paging)" "Spring MVC" "Ingress"
        priceController  = component "PriceController" "REST: POST /internal/prices — stand-in cho gRPC Catalog.GetPrice (S2S)" "Spring MVC" "InternalApi"

        # --- B. APPLICATION (Use cases) ---
        createProductUc  = component "CreateProductUc" "Tạo sản phẩm (mặc định PENDING)" "Use case"
        moderateProductUc = component "ModerateProductUc" "Admin duyệt/từ chối; publish ProductCreated đúng 1 lần khi ACTIVE lần đầu" "Use case"
        getProductUc     = component "GetProductUc" "Đọc 1 sản phẩm" "Use case"
        searchProductsUc = component "SearchProductsUc" "Truy vấn sản phẩm ACTIVE" "Use case"
        getPriceUc       = component "GetPriceUc" "Giá snapshot từ DB (nguồn sự thật) phục vụ Checkout" "Use case"
        updateSkuPriceUc = component "UpdateSkuPriceUc" "Merchant đổi giá SKU" "Use case"

        # --- C. DOMAIN ---
        catalogDomain = component "Product (Aggregate)" "Product/Variant/SKU; máy trạng thái kiểm duyệt PENDING -> ACTIVE/REJECTED; phát ProductCreated" "Domain model (POJO)"

        # --- D. OUTBOUND ---
        productOa     = component "ProductOa" "Repository<Product> qua AbstractMementoJpaOa; tra cứu SKU + tìm theo khoảng giá" "Spring Data JPA"
        catalogOutbox = component "Outbox Publisher" "Drain outbox -> Kafka (ProductCreated)" "msfw outbox"

        # --- Internal wiring ---
        prodController   -> createProductUc
        prodController   -> updateSkuPriceUc
        prodController   -> getProductUc
        adminController  -> moderateProductUc
        searchController -> searchProductsUc
        priceController  -> getPriceUc

        createProductUc   -> catalogDomain
        moderateProductUc -> catalogDomain
        updateSkuPriceUc  -> catalogDomain

        createProductUc   -> productOa
        moderateProductUc -> productOa
        updateSkuPriceUc  -> productOa
        getProductUc      -> productOa
        searchProductsUc  -> productOa
        getPriceUc        -> productOa

        productOa     -> catalogDb "Đọc/ghi" "JDBC/TLS"
        catalogOutbox -> catalogDb "Poll outbox" "JDBC/TLS"
    }
}
        # ---------- end: model/catalog-context.dsl ----------

        # ---------- inlined: model/checkout-context.dsl ----------
# ==============================================================================
# CONTEXT: Checkout  —  C4 Level 3 (Component)
# As-built: orchestrator stateless, KHÔNG có DB/JPA. Session + idempotency lock
# in-memory (Redis đã provision ở infra nhưng app CHƯA wire). Gọi sibling qua REST.
# ==============================================================================

group "Checkout Context" {

    checkoutApi = container "Checkout Service" "Saga orchestrator cho luồng đặt hàng. Không DB; session/idempotency in-memory (stand-in cho Redis). Gọi sibling services qua REST." "Spring Boot 4 / Java 21 / msfw" {

        # --- A. INBOUND ---
        chkController = component "CheckoutController" "REST: POST /v1/checkout, GET /v1/checkout/{idempotencyKey}" "Spring MVC" "Ingress"

        # --- B. APPLICATION ---
        submitCheckoutUc = component "SubmitCheckoutUc" "Saga: price -> reserve -> split -> create orders -> init escrow, có compensation ngược thứ tự (msfw CompensatingWorkflow)" "Use case"
        getSessionUc     = component "GetCheckoutSessionUc" "Đọc session terminal đã cache (owner-only, chống IDOR)" "Use case"
        orderSplitter    = component "OrderSplitter" "Tách giỏ theo merchantId -> mỗi merchant 1 pending order" "Domain service"

        # --- C. DOMAIN ---
        checkoutSaga = component "CheckoutSaga (Aggregate)" "Máy trạng thái saga PRICING -> RESERVING -> ORDERING -> ESCROWING -> REDIRECTED | FAILED" "Domain model"

        # --- D. OUTBOUND (REST clients + session store) ---
        chkCatalogClient   = component "CatalogClientOa" "RestClient -> Catalog /internal/prices" "RestClient"
        chkInventoryClient = component "InventoryClientOa" "RestClient -> Inventory reserve/release" "RestClient"
        chkOrderClient     = component "OrderClientOa" "RestClient -> Order create/cancel" "RestClient"
        chkPaymentClient   = component "PaymentClientOa" "RestClient -> Payment init-escrow" "RestClient"
        chkSession         = component "CheckoutSessionOa" "Session TTL + distributed-lock in-memory (Redis ở prod)" "In-memory" "Standin"

        # --- Internal wiring ---
        chkController -> submitCheckoutUc
        chkController -> getSessionUc

        submitCheckoutUc -> checkoutSaga
        submitCheckoutUc -> orderSplitter
        submitCheckoutUc -> chkSession
        getSessionUc     -> chkSession

        submitCheckoutUc -> chkCatalogClient   "1. Lấy giá"
        submitCheckoutUc -> chkInventoryClient "2. Reserve / Release (bù trừ)"
        submitCheckoutUc -> chkOrderClient     "3. Create / Cancel (bù trừ)"
        submitCheckoutUc -> chkPaymentClient   "4. Init escrow"
    }
}
        # ---------- end: model/checkout-context.dsl ----------

        # ---------- inlined: model/inventory-context.dsl ----------
# ==============================================================================
# CONTEXT: Inventory  —  C4 Level 3 (Component)
# As-built: JAVA/Spring Boot (KHÔNG phải Go). 1 app: REST nội bộ + Kafka consumer
# in-process. KHÔNG worker tách riêng, KHÔNG expiry cron. gRPC = REST stand-in.
# ==============================================================================

group "Inventory Context" {

    inventoryDb = container "Inventory Database" "Nguồn sự thật: stock, reservations, processed_events (idempotency). CHECK(available >= 0)." "PostgreSQL" "Database"

    inventoryApi = container "Inventory Service" "Reserve/Release/Get stock (REST nội bộ), merchant cập nhật kho (REST), Kafka consumer in-process (ProductCreated, OrderCompleted)." "Spring Boot 4 / Java 21 / msfw" {

        # --- A. INBOUND ---
        invStockController      = component "InternalStockController" "REST: POST /internal/reservations, /reservations/release, /stock-levels — stand-in cho gRPC (S2S)" "Spring MVC" "InternalApi"
        merchantStockController = component "MerchantStockController" "REST: PUT /v1/merchant/stock/{sku} (tenant-scoped)" "Spring MVC" "Ingress"
        stockEventsFacade       = component "StockEventsFacade" "Kafka consumer: onProductCreated -> InitSku, onOrderCompleted -> DeductStock (REST stand-in: /internal/events/*)" "msfw consumer"

        # --- B. APPLICATION ---
        reserveUc     = component "ReserveStockUc" "Reserve all-or-nothing; TTL 15m; idempotent theo orderRef; trạng thái HELD" "Use case"
        releaseUc     = component "ReleaseStockUc" "Nhả reservation HELD -> RELEASED" "Use case"
        initSkuUc     = component "InitSkuUc" "Khởi tạo SKU = 0 (từ ProductCreated)" "Use case"
        deductUc      = component "DeductStockUc" "Trừ kho vĩnh viễn HELD -> CONSUMED (từ OrderCompleted)" "Use case"
        merchantUpdUc = component "UpdateMerchantStockUc" "Đặt available tuyệt đối (optimistic lock)" "Use case"
        getStockUc    = component "GetStockLevelUc" "Đọc available của nhiều SKU" "Use case"

        # --- C. DOMAIN ---
        invDomain = component "Stock / Reservation (Aggregate)" "Bất biến KHÔNG OVERSELL + cân bằng available/reserved; reservation HELD -> RELEASED | CONSUMED" "Domain model"

        # --- D. OUTBOUND ---
        stockOa = component "StockOa" "Repository<Stock> qua AbstractMementoJpaOa; atomic decrement" "Spring Data JPA"

        # --- Internal wiring ---
        invStockController      -> reserveUc
        invStockController      -> releaseUc
        invStockController      -> getStockUc
        merchantStockController -> merchantUpdUc
        stockEventsFacade       -> initSkuUc
        stockEventsFacade       -> deductUc

        reserveUc     -> invDomain
        releaseUc     -> invDomain
        deductUc      -> invDomain
        initSkuUc     -> invDomain
        merchantUpdUc -> invDomain

        reserveUc     -> stockOa
        releaseUc     -> stockOa
        initSkuUc     -> stockOa
        deductUc      -> stockOa
        merchantUpdUc -> stockOa
        getStockUc    -> stockOa

        stockOa -> inventoryDb "Atomic UPDATE / read" "JDBC/TLS"
    }
}
        # ---------- end: model/inventory-context.dsl ----------

        # ---------- inlined: model/order-context.dsl ----------
# ==============================================================================
# CONTEXT: Order (OMS)  —  C4 Level 3 (Component)
# As-built: 1 Spring Boot app. Kafka consumer in-process. KHÔNG Auto-Cancel/
# Auto-Complete cron, KHÔNG worker tách riêng — auto-cancel = sự kiện trễ tự gửi
# (OrderPendingTimedOut). gRPC = REST /internal/* stand-in.
# ==============================================================================

group "Order Context" {

    orderDb = container "Order Database" "Nguồn sự thật: orders, items (price snapshot bất biến), status history, outbox." "PostgreSQL" "Database"

    orderApi = container "Order Service" "Vòng đời đơn: REST nội bộ create/cancel, REST cho buyer/merchant, Kafka consumer in-process. Outbox relay." "Spring Boot 4 / Java 21 / msfw" {

        # --- A. INBOUND ---
        orderController         = component "OrderController" "REST: GET /v1/orders/{id}, POST /{id}/confirm-delivery, /{id}/cancel" "Spring MVC" "Ingress"
        merchantOrderController = component "MerchantOrderController" "REST: POST /v1/merchant/orders/{id}/ship" "Spring MVC" "Ingress"
        internalOrderController = component "InternalOrderController" "REST: POST /internal/orders, /{id}/cancel — stand-in cho gRPC CreatePendingOrder/CancelPendingOrder (S2S)" "Spring MVC" "InternalApi"
        paymentEventsFacade     = component "PaymentEventsFacade" "Kafka consumer: PaymentReceived -> TO_SHIP, PaymentFailed -> cancel" "msfw consumer"
        orderTimeoutsFacade     = component "OrderTimeoutsFacade" "Kafka consumer: OrderPendingTimedOut -> auto-cancel nếu còn PENDING" "msfw consumer"

        # --- B. APPLICATION ---
        createOrderUc = component "CreatePendingOrderUc" "Tạo đơn PENDING (idempotent theo checkoutRef); kích hoạt timer OrderPendingTimedOut" "Use case"
        transitionUc  = component "TransitionOrderUc" "Chuyển Ship/Complete; publish OrderCompleted" "Use case"
        cancelOrderUc = component "CancelOrderUc" "Huỷ PENDING/TO_SHIP; publish OrderCancelled" "Use case"
        expireOrderUc = component "ExpirePendingOrderUc" "Huỷ nếu còn PENDING quá TTL" "Use case"
        getOrderUc    = component "GetOrderUc" "Đọc đơn theo role/owner" "Use case"

        # --- C. DOMAIN ---
        orderDomain = component "Order (Aggregate + State Machine)" "PENDING -> TO_SHIP -> SHIPPED -> COMPLETED; -> CANCELLED; price snapshot bất biến; history append-only" "Domain model"

        # --- D. OUTBOUND ---
        orderOa     = component "OrderOa" "Repository<Order> qua AbstractMementoJpaOa" "Spring Data JPA"
        orderOutbox = component "Outbox Publisher" "Drain outbox -> Kafka (OrderCompleted/Cancelled/PendingTimedOut)" "msfw outbox"

        # --- Internal wiring ---
        internalOrderController -> createOrderUc
        internalOrderController -> cancelOrderUc
        orderController         -> getOrderUc
        orderController         -> transitionUc
        orderController         -> cancelOrderUc
        merchantOrderController -> transitionUc
        paymentEventsFacade     -> transitionUc
        paymentEventsFacade     -> cancelOrderUc
        orderTimeoutsFacade     -> expireOrderUc

        createOrderUc -> orderDomain
        transitionUc  -> orderDomain
        cancelOrderUc -> orderDomain
        expireOrderUc -> orderDomain

        createOrderUc -> orderOa
        transitionUc  -> orderOa
        cancelOrderUc -> orderOa
        expireOrderUc -> orderOa
        getOrderUc    -> orderOa

        orderOa     -> orderDb "Đọc/ghi + transaction" "JDBC/TLS"
        orderOutbox -> orderDb "Poll outbox" "JDBC/TLS"
    }
}
        # ---------- end: model/order-context.dsl ----------

        # ---------- inlined: model/payment-context.dsl ----------
# ==============================================================================
# CONTEXT: Payment (Escrow)  —  C4 Level 3 (Component)
# As-built (msfw 0.3): Payment & Settlement state-stored (memento); EscrowLedger
# EVENT-SOURCED (event store + snapshot) + read model EscrowView (CQRS).
# Webhook gateway (HMAC) là REST thật; gRPC InitEscrow = REST /internal/* stand-in.
# ==============================================================================

group "Payment Context" {

    paymentDb = container "Payment Database" "Payment & Settlement (memento), event store + snapshot của EscrowLedger, read model escrow_view, outbox." "PostgreSQL" "Database,Sensitive"

    paymentDocStore = container "Settlement Docs Store" "Chứng từ đối soát ghi-một-lần (S3 Object Lock/WORM ở prod cloud; local FS ở standalone)." "AWS S3 / FS" "Database,Sensitive"

    paymentApi = container "Payment Service" "Init escrow, webhook gateway, đối soát + payout. Escrow ledger event-sourced + CQRS read model." "Spring Boot 4 / Java 21 / msfw 0.3" {

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
        # ---------- end: model/payment-context.dsl ----------

        # ---------- inlined: model/notification-context.dsl ----------
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
        internalDispatchController = component "InternalDispatchController" "REST: POST /internal/notifications/{id}/dispatch (stand-in cho worker, S2S)" "Spring MVC" "InternalApi"
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
        # ---------- end: model/notification-context.dsl ----------

            # Tài liệu văn xuôi + ADR nhúng vào hệ thống (render kèm sơ đồ trong Structurizr)
            # (bỏ khi merge: !docs documentation
 — playground không có filesystem)
            # (bỏ khi merge: !adrs adr
 — playground không có filesystem)
        }

        # ----------------------------------------------------------------------
        # 4. LUỒNG NGƯỜI DÙNG -> SERVICE (REST/JWT, không qua gateway trong code)
        # ----------------------------------------------------------------------
        buyer    -> searchController         "Tìm kiếm sản phẩm" "HTTPS/REST"
        buyer    -> prodController           "Xem sản phẩm" "HTTPS/REST"
        buyer    -> chkController            "Submit checkout" "HTTPS/REST/JWT"
        buyer    -> orderController          "Theo dõi & xác nhận đơn" "HTTPS/REST/JWT"
        merchant -> prodController           "Tạo/sửa sản phẩm & giá" "HTTPS/REST/JWT"
        merchant -> merchantStockController  "Cập nhật tồn kho" "HTTPS/REST/JWT"
        merchant -> merchantOrderController  "Ship đơn" "HTTPS/REST/JWT"
        admin    -> adminController          "Kiểm duyệt sản phẩm" "HTTPS/REST/JWT"

        # ----------------------------------------------------------------------
        # 5. ORCHESTRATION ĐỒNG BỘ (Checkout -> sibling services qua REST)
        #    Component-source -> container-target (chuẩn C4).
        # ----------------------------------------------------------------------
        chkCatalogClient   -> catalogApi   "GetPrice (/internal/prices)" "HTTPS/REST"
        chkInventoryClient -> inventoryApi "Reserve/Release (/internal/reservations)" "HTTPS/REST"
        chkOrderClient     -> orderApi     "Create/Cancel pending order (/internal/orders)" "HTTPS/REST"
        chkPaymentClient   -> paymentApi   "Init escrow (/internal/payments/escrow)" "HTTPS/REST"

        # ----------------------------------------------------------------------
        # 6. CHOREOGRAPHY BẤT ĐỒNG BỘ (qua Kafka). Hợp đồng: contracts/*.json
        # ----------------------------------------------------------------------
        catalogOutbox -> kafkaBus "publish ProductCreated"
        paymentOutbox -> kafkaBus "publish PaymentReceived / PaymentFailed / PayoutCompleted"
        orderOutbox   -> kafkaBus "publish OrderCompleted / OrderCancelled / OrderPendingTimedOut"

        kafkaBus -> stockEventsFacade    "ProductCreated (InitSku), OrderCompleted (DeductStock)"
        kafkaBus -> paymentEventsFacade  "PaymentReceived / PaymentFailed"
        kafkaBus -> orderTimeoutsFacade  "OrderPendingTimedOut (auto-cancel)"
        kafkaBus -> orderEventsFacade    "OrderCompleted (đối soát)"
        kafkaBus -> notifEventsFacade    "PaymentReceived"

        # ----------------------------------------------------------------------
        # 7. EXTERNAL EGRESS / INGRESS
        # ----------------------------------------------------------------------
        gatewayClient -> pg               "Create transaction / refund" "HTTPS/HMAC"
        pg            -> webhookController "Payment webhook" "HTTPS/HMAC"
        bankClient    -> bank             "Submit payout" "HTTPS"
        providerOa    -> provider         "Gửi email/SMS/push" "HTTPS"

        # ----------------------------------------------------------------------
        # 8. DEPLOYMENT TOPOLOGY (tầng hạ tầng — top-level, ngoài softwareSystem)
        # ----------------------------------------------------------------------

        # ---------- inlined: model/deployment.dsl ----------
# ==============================================================================
# MODEL — Deployment topology (môi trường local k3d, theo deploy/).
# Đây là tầng HẠ TẦNG: phản ánh deploy/*.yaml. Lưu ý khác biệt với container view:
#   - Redis & Elasticsearch ĐÃ provision ở infra ns nhưng app CHƯA wire (Phase C)
#     -> mô hình hoá là infrastructureNode (không có containerInstance app nào).
#   - Istio cung cấp ingress gateway + service mesh (mTLS STRICT là Phase D).
#   - Mỗi pod chạy OpenTelemetry javaagent xuất trace sang Tempo.
# ==============================================================================

deploymentEnvironment "Production (k3d)" {

    deploymentNode "Developer Workstation" "Apple Silicon (arm64), Docker Desktop ~48GB" "Docker Desktop" {

        deploymentNode "k3d cluster" "1 server + 2 agents + local registry" "k3d / k3s" {

            argocd = infrastructureNode "ArgoCD" "GitOps delivery" "ArgoCD"
            istioGw = infrastructureNode "Istio Ingress Gateway" "Edge entrypoint; service mesh (mTLS STRICT: Phase D)" "Istio"

            deploymentNode "marketplace namespace" "Istio sidecar injection" "Kubernetes Namespace" {
                deploymentNode "catalog (Deployment)"      "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  {
                    containerInstance catalogApi
                }
                deploymentNode "checkout (Deployment)"     "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  {
                    containerInstance checkoutApi
                }
                deploymentNode "inventory (Deployment)"    "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  {
                    containerInstance inventoryApi
                }
                deploymentNode "order (Deployment)"        "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  {
                    containerInstance orderApi
                }
                deploymentNode "payment (Deployment)"      "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  {
                    containerInstance paymentApi
                }
                deploymentNode "notification (Deployment)" "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  {
                    containerInstance notifApi
                }
            }

            deploymentNode "infra namespace" "" "Kubernetes Namespace" {
                deploymentNode "PostgreSQL" "catalog/inventory/order/payment/notification DBs" "StatefulSet" {
                    containerInstance catalogDb
                    containerInstance inventoryDb
                    containerInstance orderDb
                    containerInstance paymentDb
                    containerInstance notifDb
                }
                deploymentNode "Kafka" "Strimzi operator, KRaft single broker (+ schema-registry)" "Strimzi" {
                    containerInstance kafkaBus
                }
                redis = infrastructureNode "Redis" "Đã provision — app CHƯA wire (Phase C: checkout session)" "Redis"
                elastic = infrastructureNode "Elasticsearch" "Đã provision — app CHƯA wire (Phase C: catalog search)" "Elasticsearch (1 node)"
                observ = infrastructureNode "Observability" "Prometheus + Grafana + Tempo + Pushgateway" "Prometheus/Grafana/Tempo"
            }
        }
    }
}
        # ---------- end: model/deployment.dsl ----------
    }

    views {

        # ---------- inlined: views/business.dsl ----------
# ==============================================================================
# VIEWPOINT: Business / Product Owner
# Concerns: hệ thống phục vụ ai, ranh giới với thế giới bên ngoài, giá trị nghiệp vụ.
# View: C4 Level 1 — System Context.
# ==============================================================================

systemContext marketplaceSystem "SystemContext" "C4 L1: Marketplace và các thực thể ngoại vi thực sự tích hợp (Buyer/Merchant/Admin, Payment Gateway, Bank, Notification Provider)" {
    include *
    autoLayout tb
}
        # ---------- end: views/business.dsl ----------

        # ---------- inlined: views/developers.dsl ----------
# ==============================================================================
# VIEWPOINT: Development teams (mỗi team sở hữu 1 bounded context)
# Concerns: cấu trúc nội bộ service, hexagonal layering, use-case, adapter.
# View: C4 Level 3 — Component, mỗi service một view.
# ==============================================================================

component catalogApi      "CatalogComponents"      "C4 L3: Catalog Service" {
    include *
    autoLayout
}
component checkoutApi     "CheckoutComponents"     "C4 L3: Checkout Service (saga orchestrator)" {
    include *
    autoLayout
}
component inventoryApi    "InventoryComponents"    "C4 L3: Inventory Service" {
    include *
    autoLayout
}
component orderApi        "OrderComponents"        "C4 L3: Order Service (OMS)" {
    include *
    autoLayout
}
component paymentApi      "PaymentComponents"      "C4 L3: Payment Service (escrow + CQRS)" {
    include *
    autoLayout
}
component notifApi        "NotificationComponents" "C4 L3: Notification Service" {
    include *
    autoLayout
}
        # ---------- end: views/developers.dsl ----------

        # ---------- inlined: views/integration.dsl ----------
# ==============================================================================
# VIEWPOINT: Architect / Integration
# Concerns: ranh giới context, giao tiếp đồng bộ (REST) vs bất đồng bộ (Kafka),
#           luồng saga end-to-end, bù trừ (compensation).
# Views: C4 L2 Container + 2 Dynamic view (saga happy-path & compensation).
# ==============================================================================

container marketplaceSystem "Containers" "C4 L2: 6 service + datastore + Event Bus; REST đồng bộ và Kafka bất đồng bộ" {
    include *
    autoLayout lr
}

# --- Saga đặt hàng (happy path) — tận dụng quan hệ container suy ra (implied) ---
dynamic marketplaceSystem "CheckoutSaga" "Luồng đặt hàng thành công: Checkout điều phối đồng bộ rồi chuyển sang choreography qua Kafka" {
    buyer       -> checkoutApi  "POST /v1/checkout (idempotency key)"
    checkoutApi -> catalogApi   "1. Lấy giá snapshot"
    checkoutApi -> inventoryApi "2. Reserve stock (giữ chỗ)"
    checkoutApi -> orderApi     "3. Tạo pending orders (split theo merchant)"
    checkoutApi -> paymentApi   "4. Init escrow"
    paymentApi  -> pg           "5. Tạo giao dịch, trả payment URL"
    autoLayout lr
}

# --- Bù trừ saga (compensation, ngược thứ tự) khi một bước lỗi ---
dynamic marketplaceSystem "CheckoutCompensation" "Khi một bước lỗi: Checkout chạy compensation ngược thứ tự" {
    checkoutApi -> orderApi     "Cancel pending orders (newest-first)"
    checkoutApi -> inventoryApi "Release stock"
    autoLayout lr
}

# --- Choreography sau thanh toán (event-driven qua Kafka) ---
dynamic marketplaceSystem "PostPaymentFlow" "Sau khi webhook xác nhận: sự kiện lan toả qua Kafka" {
    pg         -> paymentApi   "webhook PAID (HMAC)"
    paymentApi -> kafkaBus     "publish PaymentReceived"
    kafkaBus   -> orderApi     "PaymentReceived -> TO_SHIP"
    kafkaBus   -> notifApi     "PaymentReceived -> notify merchant"
    orderApi   -> kafkaBus     "publish OrderCompleted (khi COMPLETED)"
    kafkaBus   -> paymentApi   "OrderCompleted -> đối soát + payout"
    kafkaBus   -> inventoryApi "OrderCompleted -> deduct stock"
    autoLayout lr
}
        # ---------- end: views/integration.dsl ----------

        # ---------- inlined: views/operations.dsl ----------
# ==============================================================================
# VIEWPOINT: Operations / SRE
# Concerns: topology chạy thực tế, namespace, mesh, hạ tầng dữ liệu, observability,
#           hạ tầng đã provision nhưng app chưa wire (Redis, Elasticsearch).
# View: C4 Deployment (môi trường "Production (k3d)").
# ==============================================================================

deployment marketplaceSystem "Production (k3d)" "ProdDeployment" "Triển khai as-built trên k3d: marketplace ns (6 pod + OTel) và infra ns (Postgres, Kafka, Redis*, Elasticsearch*, observability). (*) provision nhưng chưa wire." {
    include *
    autoLayout lr
}
        # ---------- end: views/operations.dsl ----------

        # ---------- inlined: views/security.dsl ----------
# ==============================================================================
# VIEWPOINT: Security
# Concerns: trust boundary, điểm tiếp nhận lưu lượng (ingress), API service-to-service,
#           dữ liệu nhạy cảm (tiền/PII/secret), hệ thống ngoài.
# View: C4 L2 Container, dùng TAG để tô đậm mối quan tâm bảo mật:
#   - Ingress       (viền cam): endpoint công khai nhận lưu lượng người dùng/đối tác
#   - InternalApi   (viền xanh): endpoint /internal/* — chỉ gọi trong mesh (kỳ vọng mTLS)
#   - Sensitive     (viền đỏ):  store/adapter xử lý tiền, PII, secret
#   - External      (xám):      hệ thống bên thứ ba
#   - Standin       (mờ/nét đứt): adapter giả lập, KHÔNG dùng cho production
# Ghi chú as-built: mTLS STRICT + authz policy (Istio) là Phase D — hiện CHƯA bật.
# Webhook gateway đã xác minh HMAC + chống replay.
# ==============================================================================

container marketplaceSystem "SecurityBoundaries" "Khung nhìn bảo mật: trust boundary, ingress, S2S API, dữ liệu nhạy cảm. Xem màu viền theo tag." {
    include *
    autoLayout lr
}
        # ---------- end: views/security.dsl ----------


        # ---------- inlined: styles.dsl ----------
styles {
    # --- C4 base element types ---
    element "Person" {
        shape Person
        background #08427b
        color #ffffff
    }
    element "Software System" {
        background #1168bd
        color #ffffff
    }
    element "Container" {
        background #438dd5
        color #ffffff
    }
    element "Component" {
        background #85bbf0
        color #000000
        shape RoundedBox
    }
    element "Database" {
        shape Cylinder
        background #228b22
        color #ffffff
    }
    element "MessageBus" {
        shape Pipe
        background #e07a5f
        color #ffffff
    }
    element "External" {
        background #999999
        color #ffffff
    }

    # --- Security concern tags (xem views/security.dsl) ---
    element "Ingress" {
        stroke #ff8c00
        strokeWidth 5
    }
    element "InternalApi" {
        stroke #2e86de
        strokeWidth 5
    }
    element "Sensitive" {
        stroke #c0392b
        strokeWidth 6
    }
    element "Standin" {
        opacity 55
        border dashed
    }

    # --- Deployment / infrastructure ---
    element "Infrastructure Node" {
        background #ffffff
        color #000000
    }
}
        # ---------- end: styles.dsl ----------
    }
}
