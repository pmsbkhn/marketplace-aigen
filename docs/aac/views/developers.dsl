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
