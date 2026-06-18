# Glossary (thuật ngữ miền)

Ngôn ngữ chung (ubiquitous language) dùng xuyên suốt code, sơ đồ và tài liệu.

## Khái niệm xuyên suốt

| Thuật ngữ | Nghĩa |
|---|---|
| **Bounded Context** | Ranh giới một mô hình miền nhất quán = một service. |
| **Aggregate** | Cụm đối tượng miền có 1 root, là đơn vị nhất quán giao dịch. |
| **Outbox** | Bảng ghi sự kiện cùng transaction nghiệp vụ; relay đẩy ra Kafka (at-least-once). |
| **Saga / Compensation** | Giao dịch dài nhiều service; lỗi thì chạy bước bù trừ ngược thứ tự. |
| **Escrow** | Quỹ giữ tiền trung gian: hold tiền buyer, release cho merchant khi đơn xong. |
| **Idempotency key** | Khoá chống xử lý trùng (checkout, webhook, consumer). |
| **Stand-in** | Hiện thực tạm cho production thật (vd ConsoleProvider thay SES). |

## Trạng thái (state machines)

| Aggregate | Các trạng thái |
|---|---|
| Product (catalog) | PENDING → ACTIVE / REJECTED (→ DEACTIVATED) |
| Reservation (inventory) | HELD → RELEASED \| CONSUMED |
| Order | PENDING → TO_SHIP → SHIPPED → COMPLETED; (PENDING/TO_SHIP) → CANCELLED |
| Payment | PENDING → PAID \| FAILED |
| Settlement / Payout | PROCESSING → COMPLETED / PENDING → SUBMITTED |
| CheckoutSaga | PRICING → RESERVING → ORDERING → ESCROWING → REDIRECTED \| FAILED |
| Notification | ACCEPTED → RENDERED → SENT \| SUPPRESSED \| FAILED |

## Sự kiện miền (Kafka — xem contracts/)

| Event | Producer → Consumers |
|---|---|
| `Catalog.ProductCreated` | catalog → inventory (InitSku) |
| `Order.OrderCompleted` | order → payment (đối soát), inventory (deduct) |
| `Order.OrderCancelled` | order → (inventory/notification — bind khi wire) |
| `Order.OrderPendingTimedOut` | order → order (timer trễ tự gửi) |
| `Payment.PaymentReceived` | payment → order (TO_SHIP), notification |
| `Payment.PaymentFailed` | payment → order (cancel) |
