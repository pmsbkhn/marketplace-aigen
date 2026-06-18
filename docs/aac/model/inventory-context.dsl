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
