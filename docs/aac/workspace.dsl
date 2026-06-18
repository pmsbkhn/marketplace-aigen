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
            !include model/shared-infra.dsl
            !include model/catalog-context.dsl
            !include model/checkout-context.dsl
            !include model/inventory-context.dsl
            !include model/order-context.dsl
            !include model/payment-context.dsl
            !include model/notification-context.dsl

            # Tài liệu văn xuôi + ADR nhúng vào hệ thống (render kèm sơ đồ trong Structurizr)
            !docs documentation
            !adrs adr
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
        !include model/deployment.dsl
    }

    views {
        !include views/business.dsl
        !include views/developers.dsl
        !include views/integration.dsl
        !include views/operations.dsl
        !include views/security.dsl

        !include styles.dsl
    }
}
