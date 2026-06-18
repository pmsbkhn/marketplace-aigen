# 2. REST `/internal/*` thay cho gRPC ở giai đoạn hiện tại

- Status: Accepted
- Date: 2026-06-18
- Context bị ảnh hưởng: Checkout, Catalog, Inventory, Order, Payment

## Context

SAD đặc tả giao tiếp đồng bộ service-to-service (S2S) bằng **gRPC + mTLS**
(Checkout.GetPrice, ReserveStock, CreatePendingOrder, InitEscrow…). Tuy nhiên dự án
ưu tiên chạy được sớm: mỗi service có profile `standalone` (H2 + outbox JSON) không cần
hạ tầng, và phải dễ test/đối chiếu hợp đồng.

## Decision

Hiện thực biên S2S bằng **REST endpoint `/internal/*`** đóng vai trò *transport
stand-in* cho gRPC:

- Catalog `POST /internal/prices`, Inventory `POST /internal/reservations`,
  Order `POST /internal/orders`, Payment `POST /internal/payments/escrow`.
- Checkout gọi qua Spring `RestClient` (`*ClientOa`).
- mTLS S2S do **Istio mesh** đảm nhiệm ở tầng hạ tầng (Phase D: STRICT), không nhúng
  trong app.

## Consequences

- (+) Chạy/test được ngay không cần stub gRPC; hợp đồng REST dễ đọc trong PR.
- (+) Đổi sang gRPC sau này chỉ ảnh hưởng adapter (`*ClientOa`, `*Controller`), không
  đụng use-case/domain — đúng tinh thần hexagonal.
- (−) Chưa có hợp đồng schema cưỡng chế cho REST S2S (khác với event đã có
  `contracts/*.json`).
- (−) Hiệu năng/streaming kém gRPC — chấp nhận ở giai đoạn này.

## Liên quan

- View: `Containers`, `CheckoutSaga` (views/integration.dsl).
- Tag `InternalApi` trong views/security.dsl đánh dấu các endpoint này.
