# ADR-0006 — Zero-Trust (mTLS/SVID + PDP/PEP), triển khai nhiều giai đoạn

| | |
| --- | --- |
| Trạng thái | Proposed |
| Ngày | 2026-06-22 |
| Phạm vi | Hệ thống (xuyên BC) |
| Decision Drivers (NFR) | NFR-SEC-01, NFR-SEC-02 |
| View/§ ảnh hưởng | [AD §6](../AD-Marketplace-AiGen.md) |
| Thay thế / bị thay thế | — |

## Context

Hệ multi-tenant, tiền thật; **không tin theo vị trí mạng** (assume breach). Cần chặn lateral movement và rò rỉ chéo tenant ngay cả khi một workload bị xâm nhập.

## Decision

Áp **Zero-Trust (NIST SP 800-207)**: danh tính workload **mTLS/SVID** (SPIFFE/SPIRE); **PDP/PEP** deny-by-default, per-request authz; **microsegmentation** (mặc định 1 BC = 1 segment); PEP ở mọi ranh giới + PoLP. Triển khai **nhiều giai đoạn** — mỗi cột mốc = 1 ADR con.

## Consequences

- (+) Chặn lateral movement; PoLP; cô lập tenant (NFR-SEC-01/02).
- (−) ZTA là hành trình: hiện mTLS + PEP biên đã có; PDP tập trung + per-request authz (GĐ2), SPIRE federation (GĐ3) **đang triển khai** (rủi ro R2); overhead vận hành.
