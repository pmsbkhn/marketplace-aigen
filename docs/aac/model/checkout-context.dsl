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
