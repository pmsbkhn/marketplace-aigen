# ADR-0012 — Bổ sung Dispute & Refund BC (lộ trình)

| | |
| --- | --- |
| Trạng thái | Proposed |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | — (business roadmap; chưa gắn NFR) |
| View/§ ảnh hưởng | [AD §1.2.2](../AD-Marketplace-AiGen.md) |
| Thay thế / bị thay thế | — |

## Context

v1.0 **chưa** xử lý tranh chấp (dispute) & hoàn tiền (refund) — Buyer/Merchant chưa có kênh khiếu nại trong hệ thống (rủi ro R5).

## Decision

Đưa **Dispute & Refund** thành một BC riêng ở **lộ trình sau v1.0** (chưa thiết kế chi tiết). Khi kích hoạt sẽ phát sinh NFR riêng (SLA xử lý dispute, đối soát refund) và bổ sung vào utility tree AD §7.1.

## Consequences

- (+) Giữ v1.0 gọn; tách rõ phạm vi.
- (−) v1.0 không xử lý dispute/refund (rủi ro R5); cần quy trình thủ công tạm thời.
