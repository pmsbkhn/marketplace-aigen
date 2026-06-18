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
