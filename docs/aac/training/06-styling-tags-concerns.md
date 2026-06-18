# Chương 6 — Styling, tag & concern

Một sơ đồ "biết nói" nhờ màu sắc và hình dạng. Trong AaC, ta **tách hình thức khỏi nội
dung**: model gắn **tag**; một file style riêng quyết định tag đó trông thế nào.

## 6.1 Vì sao tách style ra

Nếu nhúng màu vào từng element, đổi bảng màu = sửa hàng trăm chỗ. Tách ra: đổi một dòng
trong `styles.dsl` là toàn hệ thống đổi theo. Đây là nguyên tắc "separation of concerns"
áp cho chính tài liệu.

## 6.2 Gắn tag ở model

Tag là chuỗi cuối khi định nghĩa element (nhiều tag cách nhau bằng dấu phẩy). Mọi element
tự có sẵn tag mặc định (`Element`, `Container`, `Component`…); ta thêm tag tuỳ ý:

```dsl
// model/catalog-context.dsl — tag mô tả mối quan tâm bảo mật
prodController = component "ProductController" "..." "Spring MVC" "Ingress"
priceController = component "PriceController" "...stand-in cho gRPC (S2S)" "Spring MVC" "InternalApi"

// model/payment-context.dsl — dữ liệu/adapter nhạy cảm
paymentDb = container "Payment Database" "..." "PostgreSQL" "Database,Sensitive"
gatewayClient = component "GatewayClientOa" "HTTPS + HMAC..." "HTTP client" "Sensitive"

// model/notification-context.dsl — adapter giả lập
providerOa = component "ConsoleProviderOa" "...stand-in" "Stand-in" "Standin"
```

## 6.3 Định nghĩa style theo tag

`docs/aac/styles.dsl` — element-type base + concern tags:

```dsl
styles {
    # --- C4 base element types ---
    element "Person"     { shape Person; background #08427b; color #ffffff }
    element "Container"   { background #438dd5; color #ffffff }
    element "Database"    { shape Cylinder; background #228b22; color #ffffff }
    element "MessageBus"  { shape Pipe; background #e07a5f; color #ffffff }
    element "External"    { background #999999; color #ffffff }

    # --- Security concern tags ---
    element "Ingress"     { stroke #ff8c00; strokeWidth 5 }   # endpoint công khai
    element "InternalApi" { stroke #2e86de; strokeWidth 5 }   # S2S /internal/*
    element "Sensitive"   { stroke #c0392b; strokeWidth 6 }   # tiền/PII/secret
    element "Standin"     { opacity 55; border dashed }       # adapter giả lập
}
```

Thuộc tính hay dùng: `background`, `color`, `shape` (Box, RoundedBox, Cylinder, Pipe,
Person…), `stroke`, `strokeWidth`, `border` (solid/dashed/dotted), `opacity`.

## 6.4 Tag = công cụ thể hiện "concern" (42010)

Đây là chỗ tag vượt khỏi "làm đẹp": nó **mã hoá mối quan tâm của stakeholder** lên cùng một
model. View bảo mật (`SecurityBoundaries`, Ch.5.7) không cần model riêng — nó dùng **chung
model** nhưng tô đậm theo tag:

- viền **cam** `Ingress` → điểm tiếp nhận lưu lượng ngoài (bề mặt tấn công).
- viền **xanh** `InternalApi` → endpoint chỉ nên gọi trong mesh (kỳ vọng mTLS).
- viền **đỏ** `Sensitive` → nơi xử lý tiền/PII/secret (cần kiểm soát chặt).
- mờ/nét đứt `Standin` → KHÔNG dùng cho production.

→ Security đọc đúng sơ đồ "Containers" nhưng thấy ngay trust boundary và dữ liệu nhạy cảm.
Đây là **một model, nhiều concern**, song hành với "một model, nhiều view".

## 6.5 Style cho deployment

Phần tử deployment cũng style theo tag mặc định của chúng:

```dsl
element "Infrastructure Node" { background #ffffff; color #000000 }
```

(`deploymentNode`, `containerInstance`, `infrastructureNode` đều có tag mặc định để style.)

## 6.6 Perspectives (khái niệm Structurizr, nâng cao)

Ngoài tag, Structurizr có khái niệm **perspectives** — gắn nhiều "lăng kính" (Security,
Performance, Ownership…) lên cùng element kèm mô tả/giá trị, rồi chuyển lăng kính khi xem.
Về bản chất cũng là "một model, nhiều mối quan tâm". Dự án hiện dùng **tag + filtered/style**
(đủ và chắc), nhưng khi cần kiểm toán nhiều chiều, perspectives là bước tiến tiếp theo.

---

➡️ Chương 7: phần "tại sao" và văn xuôi — **ADR** và **Documentation as Code**, nhúng thẳng
vào workspace.
