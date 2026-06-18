# Chương 10 — Best practices, anti-pattern & bài tập

Chương khép lại: đúc kết kinh nghiệm và để bạn **tự tay làm** trên chính repo này.

## 9.1 Best practices (dự án này tuân theo)

1. **Một model, nhiều view.** Đừng copy element ra nhiều nơi; định nghĩa một lần, lọc lại.
2. **As-built trên hết.** Model bám code + `deploy/`; tương lai để vào `description`/ADR.
3. **Nối quan hệ đúng mức C4.** Component↔component trong cùng container; vượt biên thì tới
   container/system; bật `!impliedRelationships true` cho dynamic/deployment.
4. **Tách style khỏi model.** Tag ở model, màu ở `styles.dsl`.
5. **Modular hoá bằng `!include`.** Mỗi bounded context một file → ít đụng độ merge.
6. **Đặt `key` view ổn định & có nghĩa.** Vì tài liệu `embed:` trỏ vào nó.
7. **Ghi "tại sao" bằng ADR.** Sơ đồ nói "cái gì", ADR nói "vì sao".
8. **CI gác cổng.** `validate` + `export` để không trôi.
9. **Cập nhật tài liệu cùng PR với code.** Đây là kỷ luật quan trọng nhất.
10. **Tên hiển thị khớp tên code.** Component `ProductController` ↔ class thật → tra được.

## 9.2 Anti-pattern (tránh)

| Anti-pattern | Vì sao hại | Cách đúng |
|---|---|---|
| Vẽ box cho thứ chưa có trong code | Tài liệu nói dối → mất uy tín | Chỉ as-built; tương lai ghi ADR |
| Component-này → component-kia khác container | Phá roll-up, view L2 sai | Nối tới container (Ch.4.6) |
| Nhúng màu vào từng element | Đổi theme phải sửa khắp nơi | Tag + `styles.dsl` |
| Một "sơ đồ tổng" khổng lồ | Không ai đọc nổi | Tách theo mức C4 + theo stakeholder |
| ADR sửa tại chỗ khi đổi ý | Mất dấu vết quyết định | Viết ADR mới `Supersedes` |
| Sơ đồ export `.png` commit tay | Lạc hậu, không diff | Render từ DSL; ảnh là sản phẩm sinh ra (đã `.gitignore`) |
| Tài liệu sửa "sau" khi xong code | Trôi ngay lập tức | Cùng PR |

## 9.3 Checklist review một PR đụng `docs/aac/`

- [ ] Model có khớp thay đổi code trong cùng PR không?
- [ ] Element mới có **thật** trong code/`deploy/` không (không phải box giả)?
- [ ] Quan hệ vượt biên container nối tới *container*, không phải *component*?
- [ ] Element mới đã gắn tag phù hợp (Database/External/Ingress/Sensitive…)?
- [ ] View mới có `key` duy nhất? Tài liệu `embed:` (nếu có) trỏ đúng key?
- [ ] Quyết định lớn đã có ADR chưa?
- [ ] CI `aac.yml` xanh (validate + export)?

## 9.4 Bài tập thực hành (trên chính repo này)

> Làm trên một branch, chạy CI hoặc `structurizr/lite` để kiểm.

**Bài 1 — Đọc hiểu.** Mở `docs/aac/model/order-context.dsl`. Liệt kê: bao nhiêu inbound
adapter? Use-case nào publish sự kiện gì? Vẽ tay luồng `paymentEventsFacade → ... → orderDb`.

**Bài 2 — Thêm component.** Giả sử Catalog thêm `BrandController` (REST quản lý brand). Thêm
component đó + nối nó tới một use-case mới `ManageBrandUc → catalogDomain → productOa`. Gắn
tag `Ingress`. Chạy export, kiểm view `CatalogComponents`.

**Bài 3 — Thêm một view stakeholder.** Tạo `docs/aac/views/data.dsl` với một view chỉ tập
trung datastore (gợi ý: `container` view rồi `exclude` các app, hoặc dùng tag). `!include`
vào `workspace.dsl`. Đây là viewpoint cho DBA.

**Bài 4 — Dynamic view mới.** Viết dynamic view `ProductOnboarding`: merchant tạo sản phẩm →
admin duyệt → `ProductCreated` qua Kafka → Inventory `InitSku`. Đối chiếu với
`contracts/Catalog.ProductCreated.json`.

**Bài 5 — ADR.** Viết `adr/0006-...md` cho một quyết định giả định: "đổi REST `/internal/*`
sang gRPC ở Phase C". Nó có `Supersedes` ADR-0004 không? Ghi rõ Consequences.

**Bài 6 — Bắt lỗi CI.** Cố tình đổi tên `catalogApi` thành `catalogApiX` chỉ ở `workspace.dsl`
(không đổi nơi định nghĩa). Chạy `validate`. Đọc thông báo lỗi — đây chính là "doc drift"
mà CI chặn.

## 9.5 Đọc thêm

- C4 model: [c4model.com](https://c4model.com)
- Structurizr DSL: [docs.structurizr.com/dsl](https://docs.structurizr.com/dsl)
- ADR / MADR: [adr.github.io](https://adr.github.io)
- ISO/IEC/IEEE 42010 (architecture description)
- Tài liệu thật của dự án: bắt đầu từ [`docs/aac/README.md`](../README.md)

---

🎓 **Hết.** Bạn giờ có thể đọc, sửa, review và mở rộng bộ AaC của dự án — và áp dụng cùng
nguyên tắc cho hệ thống khác. Quan trọng nhất: **một model trung thực, nhiều view cho nhiều
người, có CI gác cổng.**
