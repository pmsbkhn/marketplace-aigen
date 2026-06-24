# ADR-0001 — Database-per-Context, không FK xuyên BC

| | |
| --- | --- |
| Trạng thái | Accepted |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | NFR-SCALE-02, NFR-SEC-02, NFR-AVAIL-01 |
| View/§ ảnh hưởng | [AD §2.2](../AD-Marketplace-AiGen.md), [AD §5](../AD-Marketplace-AiGen.md) |
| Thay thế / bị thay thế | — |

## Context

Marketplace là multi-merchant với 7 bounded context. Cần: mỗi BC **scale & deploy độc lập**, **cô lập tenant** (≥ 10.000 Merchant), và tránh coupling schema khiến đổi một BC kéo theo BC khác. Chia sẻ một DB chung sẽ tạo coupling ngầm qua FK và contention.

## Decision

Mỗi BC **sở hữu datastore riêng** (relational per-context = PostgreSQL); **không FK vật lý xuyên BC**. Tham chiếu chéo (vd `order_id` ở Payment, `product_id` ở Order) chỉ là **reference logic**, không phải khóa ngoại. Quyền sở hữu dữ liệu nêu ở AD §5; tên DB vật lý → IaC/Tech Spec.

## Consequences

- (+) BC tiến hóa & scale độc lập; ranh giới sở hữu dữ liệu rõ; nền tảng cho tenant isolation.
- (−) Không JOIN xuyên BC → cần đối soát & **eventual consistency** (xem ADR-0003); consumer phải **idempotent**; truy vấn tổng hợp phải qua API/event, không qua DB.
