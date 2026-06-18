# Chương 5 — VIEWS theo stakeholder

Một model → nhiều view. Chương này đi qua từng **loại view** của Structurizr, mỗi loại kèm
ví dụ thật và stakeholder nó phục vụ.

## 5.1 Cú pháp chung của view

```
<loại> <scope> "<key>" "<mô tả>" {
    include <...|*>      // đưa element vào
    exclude <...>        // (tuỳ chọn) loại bớt
    autoLayout <tb|lr|bt|rl>
}
```

- **`key`** là định danh view (duy nhất toàn workspace) — dùng để embed vào tài liệu.
- **`include *`** = đưa các element mặc định của scope + lân cận liên quan.
- **`autoLayout`** để Structurizr tự xếp (tb = top-bottom, lr = left-right).

## 5.2 `systemContext` — viewpoint Business (L1)

Stakeholder: PO/business. Concern: hệ thống làm gì, ai/cái gì quanh nó.
`docs/aac/views/business.dsl`:

```dsl
systemContext marketplaceSystem "SystemContext" "C4 L1: Marketplace và các thực thể ngoại vi..." {
    include *
    autoLayout tb
}
```

Ra: Buyer/Merchant/Admin + Payment Gateway/Bank/Provider quanh một hộp "Marketplace System".

## 5.3 `container` — viewpoint Integration (L2)

Stakeholder: architect, ops, dev. Concern: gồm app/DB/bus nào, nối ra sao.
`docs/aac/views/integration.dsl`:

```dsl
container marketplaceSystem "Containers" "C4 L2: 6 service + datastore + Event Bus..." {
    include *
    autoLayout lr
}
```

Các quan hệ component→container (Ch.4.6) tự cuộn thành mũi tên service↔service ở đây.

## 5.4 `component` — viewpoint Developer (L3)

Stakeholder: dev sở hữu service. Concern: cấu trúc nội bộ. Dự án có 6 view, mỗi service một
cái — `docs/aac/views/developers.dsl`:

```dsl
component catalogApi   "CatalogComponents"   "C4 L3: Catalog Service"   { include *; autoLayout }
component checkoutApi  "CheckoutComponents"  "C4 L3: Checkout Service..." { include *; autoLayout }
// ... 4 service còn lại ...
```

`scope` ở đây là **container** (`catalogApi`) → view hiện các component bên trong nó.

## 5.5 `dynamic` — luồng theo thời gian

Container/component view là **tĩnh** (ai-nối-ai). Dynamic view kể **trình tự** một kịch
bản — đánh số bước tự động. Cực hợp cho saga. `docs/aac/views/integration.dsl`:

```dsl
dynamic marketplaceSystem "CheckoutSaga" "Luồng đặt hàng thành công..." {
    buyer       -> checkoutApi  "POST /v1/checkout (idempotency key)"
    checkoutApi -> catalogApi   "1. Lấy giá snapshot"
    checkoutApi -> inventoryApi "2. Reserve stock (giữ chỗ)"
    checkoutApi -> orderApi     "3. Tạo pending orders (split theo merchant)"
    checkoutApi -> paymentApi   "4. Init escrow"
    paymentApi  -> pg           "5. Tạo giao dịch, trả payment URL"
    autoLayout lr
}
```

> Mỗi quan hệ ở đây phải **tồn tại trong model** (trực tiếp hoặc do `!impliedRelationships`
> suy ra — Ch.4.7). Đó là lý do ta bật directive đó.

Dự án có 3 dynamic view: `CheckoutSaga` (happy path), `CheckoutCompensation` (bù trừ ngược
thứ tự), `PostPaymentFlow` (choreography qua Kafka) — kể trọn vòng đời một đơn hàng.

## 5.6 `deployment` — viewpoint Operations

Stakeholder: SRE/ops. Concern: topology chạy, hạ tầng. Cần `deploymentEnvironment` ở model
(Ch.4.5). `docs/aac/views/operations.dsl`:

```dsl
deployment marketplaceSystem "Production (k3d)" "ProdDeployment" "Triển khai as-built trên k3d..." {
    include *
    autoLayout lr
}
```

`"Production (k3d)"` phải **khớp đúng** tên environment đã khai trong `model/deployment.dsl`.

> **Bài học vàng:** deployment view ≠ container view. Container view = app *wiring* hiện
> tại (REST stand-in, search trên DB). Deployment view = hạ tầng đã *provision* (có Redis,
> Elasticsearch dù app chưa dùng). Hai góc nhìn, hai sự thật khác nhau, cùng một model.

## 5.7 `filtered` & `custom` — (nâng cao)

- **`filtered`**: tạo view mới bằng cách lọc một view nền theo tag (include/exclude). Hữu ích
  để cắt lát "chỉ luồng bảo mật" từ view Containers.
- **`custom`**: vẽ hộp/mũi tên tự do, không gắn model (vd sơ đồ lộ trình). Dùng tiết kiệm.

Dự án hiện làm view bảo mật bằng **container view + tag styling** (Ch.6) cho đơn giản và
chắc chắn, thay vì `filtered` — `docs/aac/views/security.dsl`:

```dsl
container marketplaceSystem "SecurityBoundaries" "Khung nhìn bảo mật: trust boundary..." {
    include *
    autoLayout lr
}
```

Phần "đánh dấu" mối quan tâm bảo mật do **tag + style** lo (Chương 6).

## 5.8 Quy tắc đặt `key`

Mỗi view một `key` duy nhất (dự án: `SystemContext`, `Containers`, `CatalogComponents`,
`CheckoutSaga`, `ProdDeployment`, `SecurityBoundaries`…). `key` này còn dùng để **embed sơ
đồ vào tài liệu** bằng `![](embed:CheckoutSaga)` (Chương 7).

---

➡️ Chương 6: làm cho các view trên **biết nói** bằng màu sắc/hình dạng theo **tag &
concern** — đặc biệt cho góc nhìn bảo mật.
