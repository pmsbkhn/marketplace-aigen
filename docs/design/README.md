# Marketplace — Bộ tài liệu thiết kế (AD + Tech Specs)

Bộ tài liệu kiến trúc của hệ **Marketplace** viết theo chuẩn [`STD-DESIGN-DOC-v1.3`](../QuyTac-CauTruc-NoiDung-TaiLieu-ThietKe.md) (viết tay, Markdown thuần; nội dung-trước, phục vụ người viết & người đọc). Phần mã-hóa/cưỡng-chế bằng công cụ (model-as-code, drift máy) là **ngoài phạm vi** chuẩn này — do [`STD-AD-AAC`](../QuyTac-AD-ArchitectureAsCode.md) phụ trách (xem `MKT-ADR-0014`).

## Ba tầng tài liệu

| Tầng | Giữ gì (grain) | File |
| --- | --- | --- |
| **Standard** | Quy tắc viết AD/Tech Spec | `../QuyTac-CauTruc-NoiDung-TaiLieu-ThietKe.md` |
| **AD** (cấp hệ thống) | C4 **L2 / Landscape**: BC = hộp, Context Map, bề mặt hợp đồng + bảo đảm tương tác, deployment grain BC/zone, ADR register hệ thống | [`AD-Marketplace.md`](AD-Marketplace.md) |
| **Tech Spec** (per-BC) | C4 **L3 / nội bộ BC**: module & component, C&C, deployment chi tiết, domain/data, key flows, ADR context-local `ADR-<BC>-*` | [`tech-spec/`](tech-spec/) |
| Đẩy xuống nữa | field/mã lỗi đầy đủ → `/contracts`; replica/HPA/secret → IaC/Vault | — |

## Index Tech Spec

| BC | ID | Tier | Datastore | Tech Spec |
| --- | --- | --- | --- | --- |
| Catalog | `MKT-BC-catalog` | 3 | `catalog_db` + ES | [`TechSpec-Marketplace-Catalog.md`](tech-spec/TechSpec-Marketplace-Catalog.md) |
| Inventory | `MKT-BC-inventory` | 2 | `inventory_db` | [`TechSpec-Marketplace-Inventory.md`](tech-spec/TechSpec-Marketplace-Inventory.md) |
| Order | `MKT-BC-order` | 1 | `order_db` | [`TechSpec-Marketplace-Order.md`](tech-spec/TechSpec-Marketplace-Order.md) |
| Payment | `MKT-BC-payment` | 1 | `payment_db` + S3 WORM | [`TechSpec-Marketplace-Payment.md`](tech-spec/TechSpec-Marketplace-Payment.md) |
| Checkout | `MKT-BC-checkout` | 2 | Redis (no DB) | [`TechSpec-Marketplace-Checkout.md`](tech-spec/TechSpec-Marketplace-Checkout.md) |
| Notification | `MKT-BC-notification` | 3 | `notification_db` | [`TechSpec-Marketplace-Notification.md`](tech-spec/TechSpec-Marketplace-Notification.md) |

> **1 BC = 1 Tech Spec**, team của BC sở hữu. Quan hệ xuyên-BC chỉ khai ở Landscape/Context Map của AD — **không** lặp trong Tech Spec. Mỗi Tech Spec ⇄ đúng một hộp BC ở AD §3.3 (Container archetype, L2) + một dòng "Correspondence physical".

## Quy ước

- **as-is vs to-be:** code v1 dùng REST `/internal/*` stand-in + Kafka consumer in-process (`ADR-0004/0005`-aac); đích là gRPC+mTLS/Istio. Mỗi tài liệu phân biệt rõ (AD §8.4).
- **ADR:** hệ thống `MKT-ADR-NNNN` (ở AD §9) · context-local `ADR-<BC>-N` (ở Tech Spec §7). Có dãy thứ hai code-derived ở `docs/aac/adr/` — trích kèm hậu tố `-aac`.
- **`verify:`** ∈ `review · test · monitor · check · audit` — mệnh đề trọng yếu (tiền/bảo mật/tenant) **không** được chỉ `review`.
- **Nguồn sự thật:** AD/Tech Spec nêu *ngữ nghĩa + đảm bảo*; field/schema đầy đủ ở `/contracts` + migration.

## Manifest (valid-as-of 2026-06-21)

AD-Marketplace v1.0.0 · STD-DESIGN-DOC-v1.3 · nguồn SDD-MKTPLACE-CORE-v2.2 · Contracts `/contracts`@2026-06-21. **last-validated:** 2026-06-21.
