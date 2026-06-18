# Chương 8 — Hệ sinh thái & vòng đời

Chương này ghép mọi mảnh thành một **vòng đời AaC** vận hành được: nguyên tắc as-built, các
tạp phẩm sống cạnh code, render, và CI gác cổng.

## 8.1 Nguyên tắc trung thực: as-built, không vẽ box giả

Quy tắc bất di bất dịch của dự án: **chỉ mô hình hoá những gì có trong source code &
`deploy/`**. Thiết kế tương lai ghi trong `description`/ADR, **không** tạo element giả.

Vài ví dụ áp dụng (đối chiếu SAD đích vs as-built):

| SAD đích | As-built (đã sửa trong model) |
|---|---|
| gRPC S2S | REST `/internal/*` stand-in (ADR-0004) |
| Inventory/Notification viết bằng Go | Thực ra là **Java/Spring** |
| Catalog dùng Elasticsearch | Search chạy trên **PostgreSQL**; ES mới provision, chưa wire |
| Checkout dùng Redis | Session **in-memory**; Redis provision, chưa wire |
| API Gateway/Keycloak/SPIRE/OPA | **Không có trong code** → bỏ khỏi model |

> Vì sao quan trọng? Tài liệu mất uy tín ngay khi người đọc phát hiện nó nói sai về code.
> AaC chỉ có giá trị khi **trung thực**. Phần "chưa làm" được phản ánh đúng (vd Redis/ES là
> `infrastructureNode` "đã provision — chưa wire" trong deployment view).

## 8.2 Phổ tạp phẩm AaC của dự án (và vì sao không gom một chỗ)

```
docs/aac/          → sơ đồ C4 + ADR + documentation   (Structurizr)
/contracts/        → hợp đồng wire JSON cho event Kafka
/ARCHITECTURE.md   → quy ước msfw (layering, naming)
*/.../architecture/*Test.java → fitness functions (luật kiến trúc CHẠY ĐƯỢC)
/deploy/           → manifests k8s (nguồn sự thật cho deployment view)
```

Theo tinh thần AaC, **artifact sống nơi nó được dùng**: contract cạnh test sinh ra nó,
fitness test trong module test, manifest trong `deploy/`. `docs/aac/README.md` là **mục
lục** nối tất cả — không phải kho chứa trung tâm.

Hai tạp phẩm đáng chú ý:
- **Contracts** (`/contracts/*.json`): "git diff trên fixture LÀ tín hiệu đổi hợp đồng" —
  review như đổi API. Khớp đúng các quan hệ Kafka trong model.
- **Fitness functions**: luật như "domain không phụ thuộc adapter" được viết thành test,
  CI cưỡng chế. Đây là AaC ở mức **executable** — mạnh hơn cả sơ đồ vì máy kiểm được.

## 8.3 Render: nhìn thấy sơ đồ

DSL là text; cần công cụ render:

```bash
# A. Structurizr Lite — xem tương tác (Diagrams + Documentation + Decisions) ở :8080
docker run -it --rm -p 8080:8080 \
  -v "$PWD/docs/aac:/usr/local/structurizr" structurizr/lite

# B. CLI — export sang Mermaid/PlantUML/PNG để nhúng nơi khác
structurizr-cli export -workspace docs/aac/workspace.dsl -format mermaid
```

## 8.4 CI — chống trôi tài liệu

Đây là thứ biến AaC từ "tài liệu đẹp" thành "tài liệu **tin được**". `.github/workflows/aac.yml`:

```yaml
on:
  pull_request:
    paths: ['docs/aac/**', '.github/workflows/aac.yml']   # chỉ chạy khi đụng AaC
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - name: Validate Structurizr DSL workspace
        run: docker run --rm -v "$PWD:/work" -w /work structurizr/cli:latest
             validate -workspace docs/aac/workspace.dsl
      - name: Export all views (Mermaid)
        run: docker run --rm -v "$PWD:/work" -w /work structurizr/cli:latest
             export -workspace docs/aac/workspace.dsl -format mermaid -output build/aac
```

Hai tầng bắt lỗi:
- **`validate`** → lỗi cú pháp DSL, identifier gãy, `!include`/`!docs`/`!adrs` hỏng.
- **`export`** → ép dựng **mọi view** → bắt lỗi view `key`, `embed:` sai, quan hệ không tồn
  tại trong dynamic/deployment view.

→ PR sửa kiến trúc được kiểm như code; **không merge được nếu tài liệu hỏng**.

## 8.5 Vòng đời đầy đủ

```
   viết/sửa (.dsl/.md)
        │  git diff review trong PR
        ▼
   CI: validate + export  ──fail──▶ sửa
        │ pass
        ▼
   merge ▶ render (Structurizr Lite) cho người đọc
        ▲
        └─ khi code đổi (vd wire Redis thật) → cập nhật model/ + ADR trong CÙNG PR
```

Quy tắc cuối là linh hồn: **đổi code thì đổi tài liệu trong cùng PR**. Đó là cách tài liệu
không bao giờ trôi.

---

➡️ Chương 9: **quy trình & governance** — role nào sửa/duyệt, và cơ chế ràng buộc code với
AaC docs để hai bên không lệch nhau.
