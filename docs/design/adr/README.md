# ADR — Architecture Decision Records (cấp hệ thống)

> Theo `STD-DOC-v1.15` PHẦN F: mỗi quyết định nặng-kiến-trúc = **một file ADR bất biến, đánh số** (R-F2). Đây là **tập ADR cấp hệ thống** (xuyên BC). ADR nội bộ một BC nằm ở Tech Spec §7 của BC đó (vd [Checkout §7](../techspec/Checkout.md)) — inline vì ít/ngắn (R-F2).

**Convention**

- File: `ADR-XXXX-<slug>.md`; format MADR rút gọn (template F.1 của STD-DOC): *Context → Decision → Consequences* + **Decision Drivers** (NFR lái quyết định, neo về [AD §7.1.2 catalog](../AD-Marketplace-AiGen.md)).
- Lifecycle (R-F4): `Proposed → Accepted → Superseded by ADR-YYYY`. ADR `Superseded` **giữ nguyên file**, thêm con trỏ — không xóa.
- **Chỉ mục đầy đủ (kèm View/§ ảnh hưởng): [AD §A.2](../AD-Marketplace-AiGen.md)** (đây là index có thẩm quyền — R-F6). Bảng dưới chỉ để điều hướng nhanh trong thư mục.

| ADR | Quyết định | Trạng thái | Decision Drivers (NFR) |
| --- | --- | --- | --- |
| [ADR-0001](ADR-0001-db-per-context.md) | DB-per-Context, không FK xuyên BC | Accepted | NFR-SCALE-02, NFR-SEC-02 |
| [ADR-0002](ADR-0002-orchestration-checkout.md) | Orchestration (Checkout) cho luồng tiền | Accepted | NFR-PERF-01, NFR-FIN-01, NFR-FIN-03 |
| [ADR-0003](ADR-0003-event-driven-kafka.md) | Event-Driven qua Kafka; event = Published Language | Accepted | NFR-SCALE-01, NFR-AVAIL-01 |
| [ADR-0004](ADR-0004-escrow.md) | Escrow giữ tiền đến khi giao hàng hoàn tất | Accepted | NFR-FIN-01 |
| [ADR-0005](ADR-0005-worm-settlement-doc.md) | Chứng từ tài chính WORM (S3 Object Lock) | Accepted | NFR-FIN-02 |
| [ADR-0006](ADR-0006-zero-trust.md) | Zero-Trust (mTLS/SVID + PDP/PEP), nhiều giai đoạn | Proposed | NFR-SEC-01, NFR-SEC-02 |
| [ADR-0012](ADR-0012-dispute-refund-bc.md) | Bổ sung Dispute & Refund BC (lộ trình) | Proposed | — (business roadmap) |
