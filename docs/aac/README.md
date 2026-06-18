# Architecture as Code — Marketplace

Đây là **nhà chung (single home)** cho kiến trúc của hệ thống, mô tả bằng **code/text**,
versioned trong Git, review qua PR. Sơ đồ dùng **Structurizr DSL** + **mô hình C4**.

> Nguyên tắc: tài liệu phản ánh **hệ thống as-built** (đúng source code & `deploy/`).
> Phần thiết kế chưa hiện thực được ghi trong `description`/ADR, **không** vẽ box giả.

---

## 1. "Tập hợp toàn bộ" — bản đồ các tạp phẩm AaC

AaC ≠ chỉ sơ đồ. Một *architecture description* đầy đủ gồm nhiều tạp phẩm; có cái sống
ở đây, có cái **cố ý** sống cạnh code/build (và được index từ đây):

| Tạp phẩm | Vị trí | Vai trò |
|---|---|---|
| **Model + Views (C4)** | `docs/aac/` (thư mục này) | Cấu trúc tĩnh, động, triển khai |
| **ADR** | `docs/aac/adr/` | Quyết định & lý do ("tại sao") |
| **Event contracts** | [`/contracts`](../../contracts) | Hợp đồng wire JSON cho event Kafka (msfw `JsonEventContract`) |
| **Conventions** | [`/ARCHITECTURE.md`](../../ARCHITECTURE.md) | Quy ước msfw (layering, naming, cookbook) |
| **Architecture fitness tests** | `*/adapter/src/test/.../architecture/` | Luật kiến trúc **chạy được** (ArchUnit/quantum) — CI cưỡng chế |
| **Deployment manifests** | [`/deploy`](../../deploy) | Nguồn sự thật cho deployment view |

> Vì sao không gom hết vào một chỗ? Theo tinh thần AaC, **artifact sống nơi nó được
> dùng**: contracts cạnh test sinh ra nó, fitness test trong module test, manifest trong
> `deploy/`. README này là *mục lục* nối chúng — không phải kho chứa.

---

## 2. Cấu trúc thư mục

```
docs/aac/
├── README.md            ← bạn đang đọc (mục lục + hướng dẫn)
├── workspace.dsl        ← FILE GỐC: model + views + !docs + !adrs
├── styles.dsl           ← màu sắc/hình dạng theo tag
├── model/               ← MODEL (nguồn sự thật — "hệ thống TRÔNG thế nào")
│   ├── shared-infra.dsl       (Kafka)
│   ├── *-context.dsl          (6 bounded context, C4 L3 component)
│   └── deployment.dsl         (topology k3d/Istio/infra)
├── views/               ← VIEWS theo stakeholder (ISO/IEC/IEEE 42010)
│   ├── business.dsl           (System Context)
│   ├── developers.dsl         (6 Component view)
│   ├── integration.dsl        (Container + Dynamic saga)
│   ├── operations.dsl         (Deployment)
│   └── security.dsl           (Container + tag bảo mật)
├── documentation/       ← tài liệu văn xuôi nhúng (!docs) — embed sơ đồ + glossary
└── adr/                 ← Architecture Decision Records (nhúng qua !adrs)
```

Cốt lõi AaC: **một model, nhiều view**. Hệ thống định nghĩa **một lần** trong `model/`;
mỗi view chỉ là một **phép chiếu** → các view không thể mâu thuẫn nhau.

---

## 3. View cho từng stakeholder (ISO/IEC/IEEE 42010)

Mỗi *viewpoint* đóng khung *concerns* của một nhóm *stakeholder*:

| Stakeholder | Concerns | View (key) | File |
|---|---|---|---|
| Business / PO | Hệ thống làm gì, ranh giới ngoài | `SystemContext` | views/business.dsl |
| Dev team từng context | Cấu trúc nội bộ service | `*Components` (×6) | views/developers.dsl |
| Architect / Integration | Sync vs async, luồng saga | `Containers`, `CheckoutSaga`, `CheckoutCompensation`, `PostPaymentFlow` | views/integration.dsl |
| Operations / SRE | Topology chạy, infra, observability | `ProdDeployment` | views/operations.dsl |
| Security | Trust boundary, ingress, dữ liệu nhạy cảm | `SecurityBoundaries` | views/security.dsl |
| QA / Governance | Luật kiến trúc không bị phá | *(fitness tests trong CI)* | `*/adapter/.../architecture/` |

---

## 4. Render / xem sơ đồ

DSL chỉ là text — cần công cụ render. Hai cách phổ biến:

```bash
# A. Structurizr Lite — xem tương tác ở http://localhost:8080
docker run -it --rm -p 8080:8080 \
  -v "$PWD/docs/aac:/usr/local/structurizr" structurizr/lite
# (Structurizr Lite tìm workspace.dsl trong thư mục mount)

# B. structurizr-cli — export sang PlantUML / Mermaid / hình
structurizr-cli export -workspace docs/aac/workspace.dsl -format mermaid
```

Workspace đã nhúng sẵn **tài liệu** (`!docs documentation`) và **ADR** (`!adrs adr`) —
Structurizr Lite sẽ hiển thị tab **Documentation** (kèm sơ đồ embed) và **Decisions**.

## 6. CI — chống trôi tài liệu

`.github/workflows/aac.yml` chạy khi `docs/aac/**` đổi:

- `structurizr-cli validate` — bắt lỗi cú pháp DSL, identifier gãy, `!include`/`!docs`/`!adrs` hỏng.
- `structurizr-cli export` (Mermaid) — ép dựng **mọi view** → bắt lỗi view key / `embed:` / quan hệ
  không tồn tại. Sơ đồ Mermaid được upload làm artifact.

Nhờ vậy mọi PR sửa kiến trúc đều được kiểm tra như code; tài liệu không thể merge nếu hỏng.

---

## 5. As-built vs Target (lộ trình)

Sơ đồ này là **as-built**. Khác biệt chính so với SAD đích, đã ghi trong description/ADR:

- gRPC S2S → **REST `/internal/*`** (ADR-0002)
- Redis (checkout session) & Elasticsearch (catalog search) → **đã provision ở infra,
  app chưa wire** (Phase C) — thấy rõ ở `ProdDeployment`
- Worker/Cron tách riêng → **consumer in-process + sự kiện trễ** (ADR-0003)
- mTLS STRICT + authz (Istio) → **Phase D**
- Chưa hiện thực ở app: SES/Twilio/FCM, template engine, SQS/DLQ (Notification dùng stand-in)

Khi một mục được "wire" thật, cập nhật model/ + ADR trong **cùng PR** với code.
