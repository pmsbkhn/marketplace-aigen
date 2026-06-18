# 2. Dùng msfw StringIdentity cho mọi Identity miền

- Status: Accepted
- Date: 2026-06-16
- Context bị ảnh hưởng: cả 6 context (Catalog, Checkout, Inventory, Order, Payment, Notification)
- Nguồn: back-fill từ commit #31 (Phase 1 — Payment) và #32 (Phase 2 — 5 context còn lại)
  khi hợp nhất register ADR vào repo. Quyết định gốc có trước file này.

## Context

Mỗi định danh miền (PaymentId, OrderId, SkuId…) trước đây tự viết tay
`value()/equals/hashCode/toString` trên `Identity<String>` của msfw — lặp lại ~30 dòng
boilerplate mỗi lớp, dễ sai lệch (vd `EscrowLedgerId` thiếu guard non-blank), và không có
gì ngăn lập trình viên viết lại kiểu cũ.

## Decision

Mọi Identity **extends `msfw StringIdentity`** (primitive định danh chuỗi của msfw) thay vì
tự cuộn trên `Identity<String>`:

- Guard rỗng ném `InvalidArgumentException` (trước là `IllegalArgumentException`); các id
  thiếu guard nay được chuẩn hoá. Định danh có format riêng (vd `SkuCode`) giữ regex trên
  nền non-blank của StringIdentity.
- Checkout bỏ `IdempotencyKey` cục bộ, dùng `tech.vsf.ptnt.msfw.domain.core.IdempotencyKey`.
- Thêm fitness function **`msfwIdentityBase`** vào mỗi `FitnessFunctionsTest` để **không thể
  tái diễn** boilerplate (governance cưỡng chế bằng test).
- Triển khai **theo pha**: Phase 1 Payment (#31), Phase 2 năm context còn lại (#32). Bump
  `msfw 0.2.0 → 0.4.0`, `ea-archrules → 0.6.0`.

Ngoài phạm vi: `SagaState`/`Money`/`Currency` cục bộ của checkout (quyết định riêng).

## Consequences

- (+) Giảm ~519 dòng boilerplate; hành vi định danh nhất quán toàn hệ thống.
- (+) Fitness function khoá quyết định lại — code phá luật là build đỏ (xem
  [ADR-0001](0001-observability-governance-plane-split.md) về governance plane).
- (−) Nâng msfw là breaking nhẹ (đổi loại exception) — đã xử lý trong cùng PR.

## Liên quan

- Là ví dụ điển hình cho cơ chế "fitness function trói code vào kiến trúc"
  (training Chương 9). Không đổi cấu trúc C4 ở mức container/component nên model AaC giữ nguyên.
