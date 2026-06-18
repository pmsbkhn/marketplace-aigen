# Chương 1 — Architecture as Code là gì & tại sao

## 1.1 Định nghĩa

**Architecture as Code (AaC)** là cách mô tả kiến trúc phần mềm bằng **văn bản có cấu
trúc**, lưu trong **version control (Git)**, review qua **pull request**, và **render/kiểm
tra tự động** — thay vì vẽ tay trong Visio/draw.io/PowerPoint rồi export ảnh.

Nói ngắn: kiến trúc được đối xử **như code**.

## 1.2 Vấn đề của "cách cũ" (diagrams-as-images)

| Triệu chứng | Hệ quả |
|---|---|
| Sơ đồ là file `.png`/`.pptx` nhị phân | Không diff được, review PR vô nghĩa |
| Vẽ tay, không nối với code | Lạc hậu sau vài sprint, không ai tin |
| Nhiều sơ đồ rời rạc (dev vẽ khác, ops vẽ khác) | Mâu thuẫn nhau, không biết bản nào đúng |
| "Sơ đồ tổng" khổng lồ một trang | Không ai đọc nổi, không zoom theo nhu cầu |

Dự án này từng có đúng vấn đề đó: trước khi làm AaC, có các file SAD/TechSpec dạng PDF/Word
mô tả gRPC, Elasticsearch, Redis, API Gateway… nhưng **code thực tế** lại dùng REST
stand-in, search trên DB, session in-memory. Tài liệu và hiện thực **đã trôi xa nhau**.

## 1.3 AaC giải quyết thế nào

- **Text & versioned**: sơ đồ là file `.dsl` → `git diff` thấy rõ ai đổi gì.
- **Một nguồn sự thật**: định nghĩa hệ thống **một lần**, sinh ra nhiều sơ đồ.
- **Review như code**: đổi kiến trúc đi qua PR, có người duyệt.
- **Tự động hoá**: CI `validate`/`export`, chống trôi (Chương 8).

> Ví dụ thật — toàn bộ "sơ đồ" của dự án nằm trong các file text này:
> ```
> docs/aac/workspace.dsl        ← điểm vào
> docs/aac/model/*.dsl          ← 6 bounded context + hạ tầng + deployment
> docs/aac/views/*.dsl          ← các góc nhìn
> ```
> Mở `docs/aac/model/catalog-context.dsl` — bạn đang đọc "sơ đồ" Catalog, dạng text.

## 1.4 AaC ≠ chỉ là sơ đồ

Hiểu lầm phổ biến nhất: "AaC = vẽ C4 bằng code". Sai. C4/Structurizr chỉ là **lát cấu
trúc**. Một mô tả kiến trúc đầy đủ là **nhiều tạp phẩm**, tất cả đều dạng code/text:

| Tạp phẩm | Diễn đạt điều gì | Trong dự án này |
|---|---|---|
| **Sơ đồ C4** (Structurizr) | Cấu trúc tĩnh/động/triển khai | `docs/aac/model/`, `views/` |
| **ADR** | Quyết định & **lý do** ("tại sao") | `docs/aac/adr/` |
| **Contracts** | Hợp đồng API/sự kiện | `/contracts/*.json` |
| **Conventions** | Quy ước code chung | `/ARCHITECTURE.md` |
| **Fitness functions** | Luật kiến trúc **chạy được** | `*/adapter/.../architecture/*Test.java` |
| **Deployment manifests** | Topology thật | `/deploy/*.yaml` |

Triết lý: **artifact sống nơi nó được dùng**; một `README` đóng vai mục lục nối tất cả
(xem `docs/aac/README.md`). Đây là tinh thần ta theo suốt cuốn sách.

## 1.5 Lợi ích đọng lại

1. **Tin được**: tài liệu bám code, có CI gác → không lạc hậu âm thầm.
2. **Đa góc nhìn nhất quán**: một model, nhiều view, không mâu thuẫn (Chương 2).
3. **Onboarding nhanh**: người mới đọc `README` → System Context → zoom dần.
4. **Quyết định có dấu vết**: ADR giải thích "vì sao thế này", tránh tranh luận lặp lại.

---

➡️ Chương 2: nền tảng lý thuyết — **C4 model** và **ISO/IEC/IEEE 42010** — để hiểu vì sao
"một model, nhiều view" lại là trái tim của AaC.
