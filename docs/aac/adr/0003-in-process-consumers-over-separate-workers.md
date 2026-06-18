# 3. Kafka consumer in-process + sự kiện trễ thay cho cron/worker tách riêng

- Status: Accepted
- Date: 2026-06-18
- Context bị ảnh hưởng: Inventory, Order, Payment, Notification

## Context

SAD vẽ nhiều tiến trình tách rời cho mỗi context: API, Worker (Kafka consumer),
Outbox Relay, Scheduler/CronJob (auto-cancel đơn PENDING, auto-complete đơn SHIPPED,
expiry reservation, reindex…). Mỗi tiến trình thêm = thêm deployment, thêm vận hành.

## Decision

Mỗi context triển khai **một** Spring Boot app duy nhất:

- **Kafka consumer chạy in-process** qua msfw consumption pipeline (`*EventsFacade`),
  không tách worker. Profile `standalone` thay Kafka bằng REST `/internal/events/*`.
- **Outbox relay** là thành phần in-process của msfw (`@EventPublishHandler` +
  `JsonEventStoreProcessor`), không phải tiến trình riêng.
- **Auto-cancel đơn PENDING** dùng **sự kiện trễ tự gửi** `OrderPendingTimedOut`
  (msfw `DelayedEvent`, outbox giữ đến hạn) thay cho CronJob.

## Consequences

- (+) Ít deployment, ít chỗ hỏng, vận hành đơn giản; vẫn giữ ranh giới hexagonal.
- (+) Auto-cancel không cần scheduler/clock riêng → đỡ vấn đề đồng bộ thời gian.
- (−) Consumer và API chia sẻ tài nguyên trong một process → muốn scale độc lập phải
  refactor (tách deployment sau).
- (−) Timer "fire-and-forget": timer cũ (stale) bị bỏ qua im lặng — đã xử lý trong
  ExpirePendingOrderUc (chỉ huỷ nếu còn PENDING).

## Liên quan

- View: `OrderComponents` (orderTimeoutsFacade), các `*Components` khác.
- contracts/Order.OrderPendingTimedOut.json (self-consumed).
