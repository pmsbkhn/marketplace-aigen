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
        moderateProductUc= component "ModerateProductUc" "Admin duyệt/từ chối; publish ProductCreated đúng 1 lần khi ACTIVE lần đầu" "Use case"
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
