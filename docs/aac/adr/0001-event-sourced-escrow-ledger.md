# 1. EscrowLedger event-sourced + CQRS read model

- Status: Accepted
- Date: 2026-06-18
- Context bị ảnh hưởng: Payment

## Context

Escrow giữ tiền của buyer rồi giải phóng cho merchant sau khi đơn hoàn tất. Đây là
miền **tài chính**: cần lịch sử kiểm toán bất biến (ai giữ/giải phóng bao nhiêu, khi
nào), khả năng tái dựng số dư tại mọi thời điểm, và đối soát. Các aggregate khác trong
hệ thống (Payment, Settlement, Order, Stock…) dùng kiểu **state-stored** (memento) qua
`AbstractMementoJpaOa` là đủ.

## Decision

Mô hình hoá **EscrowLedger** theo **event sourcing**: trạng thái là chuỗi sự kiện
`EscrowOpened → FundsHeld* → FundsReleased*` lưu trong **msfw JPA event store + snapshot**.
Bổ sung **CQRS read side**: `EscrowProjector` chiếu sự kiện in-process sang read model
`EscrowView` (bảng `escrow_view`) để truy vấn nhanh.

Payment & Settlement **vẫn** state-stored — không event-source toàn bộ context, chỉ
nơi thực sự cần lịch sử tài chính.

## Consequences

- (+) Audit trail bất biến, tái dựng số dư, hợp với yêu cầu đối soát tài chính.
- (+) Đọc tách khỏi ghi (CQRS) → query view không đụng event store.
- (−) Hai phong cách persistence trong cùng một context → lập trình viên phải nắm rõ
  ranh giới (đã ghi trong model/payment-context.dsl).
- (−) Cần quản lý snapshot/upcasting khi schema sự kiện đổi.

## Liên quan

- View: `PaymentComponents` (model/payment-context.dsl) — `escrowLedgerService`, `escrowStore`.
- Test: `payment` quantum fitness trên sync-boundary + event store (commit #29).
