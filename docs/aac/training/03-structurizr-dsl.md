# Chương 3 — Structurizr DSL: cú pháp cốt lõi

Structurizr là công cụ AaC do Simon Brown (cha đẻ C4) làm. **DSL** là ngôn ngữ text của
nó. Chương này là "ngữ pháp" — đủ để đọc hiểu mọi file trong `docs/aac/`.

## 3.1 Khung xương: `workspace { model { } views { } }`

Mọi thứ nằm trong một `workspace`, chia hai khối lớn:

- `model { }` — **ĐỊNH NGHĨA**: có những gì, nối với nhau ra sao ("hệ thống TRÔNG thế nào").
- `views { }` — **TRÌNH BÀY**: vẽ cái gì, layout, màu sắc.

Khung thật của dự án (rút gọn từ `docs/aac/workspace.dsl`):

```dsl
workspace "E-commerce Marketplace" "Kiến trúc as-built ..." {

    model {
        !impliedRelationships true
        buyer = person "Buyer" "..."
        pg    = softwareSystem "Payment Gateway" "..." "External"

        marketplaceSystem = softwareSystem "Marketplace System" "..." {
            !include model/catalog-context.dsl
            // ... 5 context còn lại ...
            !docs documentation
            !adrs adr
        }

        // quan hệ liên-service
        chkCatalogClient -> catalogApi "GetPrice (/internal/prices)" "HTTPS/REST"

        !include model/deployment.dsl
    }

    views {
        !include views/business.dsl
        // ... các view khác ...
        !include styles.dsl
    }
}
```

## 3.2 Định nghĩa element

Cú pháp chung: `<id> = <loại> "<Tên>" "<Mô tả>" "<Công nghệ>" "<Tags>"`
(các tham số sau Tên là tuỳ chọn, theo thứ tự).

| Loại | Cú pháp | Ví dụ trong dự án |
|---|---|---|
| `person` | `id = person "Tên" "Mô tả"` | `buyer = person "Buyer" "..."` |
| `softwareSystem` | `id = softwareSystem "Tên" "Mô tả" "Tags"` | `pg = softwareSystem "Payment Gateway" "..." "External"` |
| `container` | `id = container "Tên" "Mô tả" "Công nghệ" "Tags"` | `catalogDb = container "Catalog Database" "..." "PostgreSQL" "Database"` |
| `component` | `id = component "Tên" "Mô tả" "Công nghệ" "Tags"` | `prodController = component "ProductController" "..." "Spring MVC" "Ingress"` |

> **`id` vs "Tên"**: `id` (vd `catalogApi`) là *tên biến* để nối quan hệ trong DSL — không
> hiển thị. "Tên" (vd `"Catalog Service"`) là nhãn hiển thị trên sơ đồ.

Lồng nhau bằng dấu ngoặc nhọn: container chứa component, software system chứa container:

```dsl
catalogApi = container "Catalog Service" "..." "Spring Boot 4 / Java 21 / msfw" {
    prodController = component "ProductController" "..." "Spring MVC" "Ingress"
    productOa      = component "ProductOa" "..." "Spring Data JPA"
}
```

## 3.3 Quan hệ (relationship)

Cú pháp: `<nguồn> -> <đích> "<Mô tả>" "<Công nghệ>"`

```dsl
productOa -> catalogDb "Đọc/ghi" "JDBC/TLS"          // trong model/catalog-context.dsl
chkCatalogClient -> catalogApi "GetPrice (/internal/prices)" "HTTPS/REST"  // trong workspace.dsl
catalogOutbox -> kafkaBus "publish ProductCreated"   // qua Kafka
```

`id` ở hai đầu phải đã được định nghĩa (ở bất kỳ file nào được `!include` trước đó —
identifier là toàn cục). Quy tắc *nên* nối ở mức nào → Chương 4.

## 3.4 `group` — gom nhóm trực quan

`group "Tên" { ... }` gom element lại (vẽ thành khung bao), không phải một element thật.
Dự án dùng group để khoanh mỗi bounded context:

```dsl
// model/catalog-context.dsl
group "Catalog Context" {
    catalogDb = container "Catalog Database" ...
    catalogApi = container "Catalog Service" ... { ... }
}
```

## 3.5 Tags + Styles — tách hình thức khỏi nội dung

Element gắn **tag** (chuỗi cuối khi định nghĩa); `styles.dsl` quy định tag đó tô màu/hình
gì. Đổi diện mạo toàn hệ thống = sửa style, không đụng model. (Chi tiết Chương 6.)

```dsl
// gắn tag khi định nghĩa
kafkaBus = container "Event Bus" "..." "Apache Kafka (Strimzi)" "MessageBus"

// styles.dsl quyết định "MessageBus" trông thế nào
element "MessageBus" { shape Pipe; background #e07a5f; color #ffffff }
```

## 3.6 Directive (lệnh `!`)

| Directive | Tác dụng | Dùng ở đâu trong dự án |
|---|---|---|
| `!include <path>` | Chèn file khác (theo đường dẫn tương đối) | gom model/views modular |
| `!docs <dir>` | Nhúng tài liệu markdown vào element | `!docs documentation` (Ch.7) |
| `!adrs <dir>` | Nhúng ADR vào element | `!adrs adr` (Ch.7) |
| `!impliedRelationships true` | Tự suy quan hệ cấp cha từ quan hệ cấp con | cần cho dynamic/deployment view (Ch.4) |

Ví dụ `!include` tạo cấu trúc nhiều file (best practice cho dự án nhiều người):

```dsl
// workspace.dsl gom 8 file model
!include model/shared-infra.dsl
!include model/catalog-context.dsl
// ...
```

## 3.7 Bình luận

`#` hoặc `//` cho một dòng; `/* ... */` cho khối. Dự án dùng cả `#` (header) và `//`.

---

➡️ Chương 4: dùng các viên gạch trên để **xây dựng MODEL** hoàn chỉnh — kèm quy tắc nối
quan hệ "chuẩn C4" mà dự án tuân theo.
