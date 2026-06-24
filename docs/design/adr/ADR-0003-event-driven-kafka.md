# ADR-0003 — Event-Driven qua Kafka; event là Published Language

| | |
| --- | --- |
| Trạng thái | Accepted |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | NFR-SCALE-01, NFR-AVAIL-01 |
| View/§ ảnh hưởng | [AD §2.3](../AD-Marketplace-AiGen.md), [AD §3.3](../AD-Marketplace-AiGen.md) |
| Thay thế / bị thay thế | — |

## Context

Các luồng không cần đồng bộ (ProductCreated, PaymentReceived, OrderCompleted) cần **liên kết lỏng** để BC scale & chịu lỗi độc lập, không kéo nhau down theo kiểu gọi đồng bộ.

## Decision

Dùng **Kafka làm event bus** (capability: event bus / async). Event là **Published Language** — hợp đồng công khai (AsyncAPI + schema registry), không phải phụ phẩm publisher. Đảm bảo **at-least-once**, ordering per key (vd `merchant_id`/`orderId`), **consumer idempotent**.

## Consequences

- (+) BC độc lập, throughput cao (partition theo `merchant_id`); chịu lỗi tốt hơn.
- (−) **Eventual consistency** → cần idempotency, DLQ, reconcile & monitoring (lệch tạm thời giữa BC — rủi ro R3).
