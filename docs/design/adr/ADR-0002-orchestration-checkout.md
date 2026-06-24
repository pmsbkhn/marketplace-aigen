# ADR-0002 — Orchestration (Checkout) cho luồng tiền

| | |
| --- | --- |
| Trạng thái | Accepted |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | NFR-PERF-01, NFR-FIN-01, NFR-FIN-03 |
| View/§ ảnh hưởng | [AD §2.2](../AD-Marketplace-AiGen.md), [AD §3.1.1](../AD-Marketplace-AiGen.md), [Checkout Tech Spec](../techspec/Checkout.md) |
| Thay thế / bị thay thế | — |

## Context

Luồng checkout chạm tiền: lấy giá → giữ kho → tách đơn theo Merchant → tạo pending order → khởi tạo escrow. Cần **kiểm soát thứ tự** và **compensation tập trung** khi một bước lỗi (tránh reservation/order mồ côi). Choreography (thuần event) khó đảm bảo thứ tự rollback và khó phát hiện mồ côi cho luồng tiền.

## Decision

Dùng **Orchestration**: Checkout là BC điều phối một **saga đồng bộ** gọi Catalog/Inventory/Order/Payment; bước lỗi → compensation **ngược thứ tự** tập trung tại Checkout. Choreography vẫn dùng cho luồng *không chạm tiền* (event-driven, ADR-0003).

## Consequences

- (+) Compensation tập trung, dễ phát hiện & ngăn mồ côi; kiểm soát được ngân sách độ trễ end-to-end (NFR-PERF-01).
- (−) Checkout là **điểm điều phối**: down → không checkout được (chấp nhận — Tier-2; mitigate bằng HPA + canary).
