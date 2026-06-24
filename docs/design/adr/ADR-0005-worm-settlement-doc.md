# ADR-0005 — Chứng từ tài chính bất biến (S3 Object Lock / WORM)

| | |
| --- | --- |
| Trạng thái | Accepted (IAM policy literal — **TBD**, xem R1) |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | NFR-FIN-02 |
| View/§ ảnh hưởng | [AD §5.2.3](../AD-Marketplace-AiGen.md), [AD §6.3](../AD-Marketplace-AiGen.md) |
| Thay thế / bị thay thế | — |

## Context

Chứng từ đối soát/đối soát tài chính phải **bất biến** để tuân thủ (luật kế toán, retention 10 năm) — không được sửa/xóa kể cả owner.

## Decision

Lưu chứng từ trên **S3 Object Lock (WORM)**, chế độ *write-once, deny overwrite/delete*; cross-region replication cho DR. Nguyên tắc đã chốt; **IAM policy literal chờ ADR/issue riêng** (R1).

## Consequences

- (+) Tuân thủ + audit bất biến; chống sửa chứng từ.
- (−) Retention cứng 10 năm; **IAM policy chi tiết TBD** → cần gate review trước khi bật prod (rủi ro R1).
