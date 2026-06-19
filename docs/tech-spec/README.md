# Tech Specs — thiết kế chi tiết theo Bounded Context

Nhà chung cho **Tech Spec per-BC**: tách thiết kế nội bộ từng bounded context ra **file riêng,
do team của BC đó sở hữu** — thay vì nhồi vào SDD trung tâm. Đây là tầng 3 của mô hình 3 tài liệu
(Standard → **SDD/AD** → **Tech Spec**).

## Luật tầng (theo SDD-MKTPLACE-CORE-v2.2 §2.2.3)

| Tầng | Giữ gì | Ở đâu |
| --- | --- | --- |
| **AD (SDD)** | **C4 L2 / Landscape**: BC = hộp, Context Map, bề mặt hợp đồng + bảo đảm tương tác, deployment grain BC/zone, ADR register hệ thống | `docs/SDD-MKTPLACE-CORE-v2.2.md` |
| **Tech Spec (file này)** | **C4 L3 / nội bộ BC**: module & component, C&C, deployment chi tiết per-BC, domain/data nội bộ, key flows, quyết định **context-local** (`ADR-<BC>-*`) | `docs/tech-spec/TechSpec-Marketplace-<BC>.md` |
| Đẩy xuống nữa | field/mã lỗi đầy đủ → OpenAPI/proto · replica/HPA/secret → IaC/Vault | `/contracts`, IaC |

**Vì sao tách:** L3 đổi liên tục (Tier Test) + không ai ngoài BC phụ thuộc (Dependency Test) →
để trong AD gây stale & **nhiều team dẫm đè một file**. Tách ra file riêng = mỗi team gác phần mình.

## Correspondence
Mỗi Tech Spec ⇄ đúng một **hộp BC** ở SDD §2.2.1 và một dòng ở **bảng correspondence SDD §2.4**.
Quan hệ xuyên-BC chỉ khai ở Landscape/Context Map của AD, **không** lặp trong Tech Spec.

## Index

| BC | Tech Spec | Trạng thái |
| --- | --- | --- |
| Checkout | [`TechSpec-Marketplace-Checkout.md`](TechSpec-Marketplace-Checkout.md) | ✅ pilot (căn theo v2.2) |
| Payment | `TechSpec-Marketplace-Payment.md` | ⏳ |
| Catalog · Inventory · Order · Identity · Notification | `TechSpec-Marketplace-<BC>.md` | ⏳ theo cùng khuôn |

## Khuôn một Tech Spec (theo Checkout làm mẫu)
`1. Context & Scope` (ranh giới BC, trust boundary, goals/non-goals) · `2. Requirements (FR/NFR-SLO)` ·
`3. Design overview` (3.1 Module view · 3.2 C&C · 3.3 Deployment per-BC) · `4. Interfaces & data`
(API ngữ nghĩa→OpenAPI · domain model · data/schema nội bộ · config · personal data) · `5. Key flows`
(happy · compensation · fail-fast) · `6. Operations & Resilience (delta)` · `7. Decisions context-local
(ADR-<BC>-*) + cross-cutting` · `8. Test strategy (+ fitness functions)` · `9. Open questions`.

## Hướng tới AaC (ADR-0014)
Khi chuyển model-as-code: mỗi Tech Spec ↔ một **Structurizr workspace per-BC** `extends` base landscape,
`!element <bc>` bơm L2/L3 — correspondence qua trùng identifier. Đã kiểm chứng cơ chế ở
`docs/aac/large-system/` (nhánh thí nghiệm). Khi đó phần cấu trúc/sơ đồ của Tech Spec sinh từ model.
