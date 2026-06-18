# Architecture as Code — Sách training (qua dự án Marketplace)

> Một cuốn cầm tay dạy **Architecture as Code (AaC)** từ con số 0, dùng **chính bộ tài
> liệu AaC của dự án này** (`docs/aac/`) làm ví dụ sống cho từng khái niệm.

## Cuốn sách này dành cho ai

- Lập trình viên / kiến trúc sư muốn mô tả kiến trúc bằng **code** thay vì vẽ tay.
- Người mới nghe tới **C4 model**, **Structurizr**, **ISO/IEC/IEEE 42010** và muốn hiểu
  cách chúng khớp vào nhau.
- Thành viên dự án Marketplace muốn đọc/đóng góp vào `docs/aac/`.

Không cần biết trước Structurizr. Có biết Git + đọc được markdown là đủ.

## Cách dùng

Mỗi chương gồm: **(1)** khái niệm → **(2)** cú pháp → **(3)** ví dụ thật trích từ
`docs/aac/` của dự án → **(4)** "tại sao quan trọng". Đọc tuần tự lần đầu; sau đó tra
theo nhu cầu.

Tất cả ví dụ là **as-built** — bạn có thể mở file thật cạnh sách để đối chiếu, và chạy
`structurizr/lite` để thấy sơ đồ render ra.

## Mục lục

| Ch. | Tiêu đề | Bạn sẽ học |
|---|---|---|
| [01](01-aac-la-gi.md) | AaC là gì & tại sao | Định nghĩa, so với cách cũ, lợi ích, phổ tạp phẩm |
| [02](02-c4-va-42010.md) | Nền tảng: C4 & ISO 42010 | 4 mức C4; stakeholder/concern/viewpoint/view; "một model, nhiều view" |
| [03](03-structurizr-dsl.md) | Structurizr DSL | Cú pháp cốt lõi: workspace/model/views, element, relationship, tag, directive |
| [04](04-xay-dung-model.md) | Xây dựng MODEL | person → system → container → component → deployment; quy tắc quan hệ C4 |
| [05](05-views-theo-stakeholder.md) | VIEWS theo stakeholder | systemContext/container/component/dynamic/deployment/filtered; ánh xạ 42010 |
| [06](06-styling-tags-concerns.md) | Styling, tag & concern | Tách hình thức khỏi nội dung; tag bảo mật; perspective |
| [07](07-adr-va-documentation.md) | ADR & Documentation as Code | MADR; `!adrs`; `!docs`; glossary / ubiquitous language |
| [08](08-he-sinh-thai-va-ci.md) | Hệ sinh thái & vòng đời | As-built vs target; contracts; fitness functions; CI validate; render |
| [09](09-quy-trinh-va-governance.md) | Quy trình & Governance | Role (RACI) sửa/duyệt; 5 tầng ràng buộc code↔docs; CODEOWNERS; chốt trong vòng đời |
| [10](10-best-practices-va-bai-tap.md) | Best practices & bài tập | Anti-pattern; checklist review; bài tập trên chính repo này |

## Bản đồ nhanh tới tài liệu thật của dự án

```
docs/aac/
├── workspace.dsl     → Ch.03, 04   (file gốc)
├── styles.dsl        → Ch.06
├── model/            → Ch.04        (nguồn sự thật)
├── views/            → Ch.05
├── documentation/    → Ch.07
├── adr/              → Ch.07
└── README.md         → Ch.08        (mục lục tạp phẩm)
```
