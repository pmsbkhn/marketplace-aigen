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
