# ADR-0004 — Escrow giữ tiền đến khi giao hàng hoàn tất

| | |
| --- | --- |
| Trạng thái | Accepted |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | NFR-FIN-01 |
| View/§ ảnh hưởng | [AD §3](../AD-Marketplace-AiGen.md), [AD §5](../AD-Marketplace-AiGen.md) |
| Thay thế / bị thay thế | — |

## Context

Tiền Buyer trả là **tiền thật**; phải an toàn đến khi đơn hoàn tất để bảo vệ Buyer trong sàn multi-merchant (mục tiêu M2: 0 lệch tiền).

## Decision

Áp **escrow**: Payment giữ tổng tiền giỏ đến khi `OrderCompleted`, sau đó tính hoa hồng → release → payout về Merchant. Một escrow cho tổng giỏ (chi tiết per-BC: [ADR-CHK-2](../techspec/Checkout.md)).

## Consequences

- (+) Bảo vệ Buyer; nền tảng cho đối soát & payout đúng (NFR-FIN-01/02).
- (−) Phức tạp hóa Settlement (phân bổ khi settle); mọi thao tác escrow/payout phải **idempotent** + audit.
