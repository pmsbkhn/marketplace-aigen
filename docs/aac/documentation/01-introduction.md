# Giới thiệu & Phạm vi

Marketplace là sàn TMĐT **multi-merchant**: buyer mua từ nhiều merchant trong một giỏ,
thanh toán giữ qua **escrow**, tiền chỉ payout cho merchant sau khi đơn hoàn tất.

Hệ thống gồm **6 service** Spring Boot/msfw (kiến trúc hexagonal): catalog, checkout,
inventory, order, payment, notification — giao tiếp **đồng bộ qua REST** và **bất đồng
bộ qua Kafka**.

> **Nguyên tắc tài liệu:** đây là kiến trúc **as-built** — phản ánh đúng source code và
> `deploy/`. Phần SAD chưa hiện thực được ghi trong description/ADR, không vẽ box giả.
> Khác biệt as-built vs target xem mục [Triển khai](#) và các ADR.

## Bối cảnh hệ thống (C4 L1)

![System Context](embed:SystemContext)

- **Actor**: Buyer, Merchant, Platform Admin.
- **Hệ thống ngoài thực sự tích hợp**: Payment Gateway (HMAC), Merchant Bank (payout),
  Notification Provider (SES/Twilio/FCM — hiện là stand-in).
