# Chương 4 — Xây dựng MODEL

Model là **nguồn sự thật duy nhất**. Chương này đi từ ngoài vào trong: actor → hệ thống →
container → component → deployment, rồi tới **quy tắc nối quan hệ chuẩn C4**.

## 4.1 Actor (person) & hệ thống ngoài (external)

Bắt đầu từ biên: ai dùng, hệ thống nào bên ngoài. Trong `workspace.dsl`:

```dsl
buyer    = person "Buyer" "Mua sắm, checkout, theo dõi & xác nhận đơn."
merchant = person "Merchant" "Quản lý sản phẩm/giá, cập nhật kho, ship đơn."
admin    = person "Platform Admin" "Kiểm duyệt sản phẩm."

pg       = softwareSystem "Payment Gateway" "Cổng thanh toán bên thứ ba (HMAC)..." "External"
bank     = softwareSystem "Merchant Bank" "Ngân hàng nhận payout..." "External"
provider = softwareSystem "Notification Provider" "SES/Twilio/FCM ở prod..." "External"
```

> **Nguyên tắc trung thực (rất quan trọng):** chỉ mô hình hoá external **thực sự được tích
> hợp trong code**. Dự án **bỏ** courier, API Gateway, Keycloak, SPIRE, OPA, CDN khỏi
> model vì code chưa nối tới chúng — không vẽ box giả. (Chi tiết Chương 8.)

## 4.2 Software system & bounded context (group)

Hệ thống lõi là một `softwareSystem`, bên trong `!include` từng context:

```dsl
marketplaceSystem = softwareSystem "Marketplace System" "..." {
    !include model/shared-infra.dsl       // Kafka
    !include model/catalog-context.dsl    // mỗi file = 1 group "X Context"
    !include model/checkout-context.dsl
    // ...
}
```

Mỗi file context khoanh một `group` → một bounded context (DDD). Tách file giúp mỗi team
sửa phần mình, ít đụng độ merge.

## 4.3 Container

Bên trong context là các container (app, DB). Ví dụ Catalog có 1 app + 1 DB
(`model/catalog-context.dsl`):

```dsl
group "Catalog Context" {
    catalogDb  = container "Catalog Database" "Nguồn sự thật: products..." "PostgreSQL" "Database"
    catalogApi = container "Catalog Service" "CRUD, kiểm duyệt, tìm kiếm, giá..." "Spring Boot 4 / Java 21 / msfw" {
        // ... component ...
    }
}
```

So sánh nhanh đặc thù vài service (mỗi cái dạy một điều):

| Service | Điểm đặc biệt as-built |
|---|---|
| `checkoutApi` | **Không có DB** — session in-memory; là saga orchestrator |
| `inventoryApi` | Java (không phải Go như SAD); consumer Kafka **in-process** |
| `paymentApi` | Có thêm `paymentDocStore` (S3/WORM); EscrowLedger **event-sourced** |

## 4.4 Component (C4 L3)

Bên trong container, chia theo **hexagonal**: inbound → use-case → domain → outbound. Trích
`model/catalog-context.dsl`:

```dsl
// --- A. INBOUND (REST) ---
prodController = component "ProductController" "REST: POST /v1/products..." "Spring MVC" "Ingress"
// --- B. APPLICATION (Use cases) ---
createProductUc = component "CreateProductUc" "Tạo sản phẩm (mặc định PENDING)" "Use case"
// --- C. DOMAIN ---
catalogDomain = component "Product (Aggregate)" "...phát ProductCreated" "Domain model (POJO)"
// --- D. OUTBOUND ---
productOa = component "ProductOa" "Repository<Product>..." "Spring Data JPA"
```

Mỗi component khớp một class/khối thật trong code → ai đọc sơ đồ cũng tìm được trong repo.

## 4.5 Deployment environment (cho deployment view)

Topology hạ tầng là một `deploymentEnvironment` **top-level** (ngang hàng softwareSystem,
không lồng trong nó). Trích `model/deployment.dsl`:

```dsl
deploymentEnvironment "Production (k3d)" {
    deploymentNode "k3d cluster" "1 server + 2 agents..." "k3d / k3s" {
        deploymentNode "marketplace namespace" "Istio sidecar injection" "Kubernetes Namespace" {
            deploymentNode "payment (Deployment)" "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)" {
                containerInstance paymentApi          // <-- instance của container đã định nghĩa
            }
        }
        deploymentNode "infra namespace" "" "Kubernetes Namespace" {
            deploymentNode "PostgreSQL" "..." "StatefulSet" {
                containerInstance paymentDb
            }
            redis   = infrastructureNode "Redis" "Đã provision — app CHƯA wire (Phase C)" "Redis"
            elastic = infrastructureNode "Elasticsearch" "Đã provision — app CHƯA wire..." "Elasticsearch (1 node)"
        }
    }
}
```

Hai khái niệm mới:
- **`containerInstance <id>`**: một *thể hiện chạy* của container đã định nghĩa ở model. Đây
  là cầu nối khiến deployment view và container view **luôn khớp nhau**.
- **`infrastructureNode`**: hạ tầng không phải container của ta (Redis/ES provision sẵn
  nhưng app chưa dùng) → mô hình trung thực: có ở hạ tầng, chưa có instance app.

## 4.6 QUY TẮC VÀNG: nối quan hệ ở đúng mức C4

Đây là lỗi người mới hay mắc. Quy tắc:

> **Component chỉ nối component trong CÙNG container. Vượt biên container thì nối tới
> container/system, KHÔNG nối thẳng component-này-sang-component-kia của container khác.**

Vì sao? Structurizr **tự "cuộn" (roll-up)**: quan hệ component→container sẽ tự hiện thành
container→container ở view L2. Viết một lần, dùng cho cả hai mức.

Áp dụng trong dự án — Checkout gọi Catalog: nguồn là *component* `chkCatalogClient`, đích
là *container* `catalogApi` (không phải `priceController`):

```dsl
// trong container thì component nối component:
submitCheckoutUc -> chkCatalogClient "1. Lấy giá"     // (model/checkout-context.dsl)

// vượt biên container thì component nối CONTAINER:
chkCatalogClient -> catalogApi "GetPrice (/internal/prices)" "HTTPS/REST"   // (workspace.dsl)
```

Ở view `Containers`, quan hệ trên tự cuộn thành `checkoutApi -> catalogApi`.

## 4.7 `!impliedRelationships true` — vì sao cần

Roll-up ở trên là cho **rendering view**. Nhưng **dynamic view** và **deployment view** cần
quan hệ container→container **tồn tại thật trong model** để vẽ mũi tên. Directive này tự
sinh các quan hệ cha đó từ quan hệ con:

```dsl
model {
    !impliedRelationships true   // chkCatalogClient->catalogApi tự suy ra checkoutApi->catalogApi
    ...
}
```

Nhờ nó, dynamic view ở Chương 5 mới "sequence" được `checkoutApi -> catalogApi`.

---

➡️ Chương 5: từ model này, dựng **các view cho từng stakeholder**.
