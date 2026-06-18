# Architecture Decision Records (ADR)

Mỗi ADR ghi lại **một quyết định kiến trúc** và lý do — phần "tại sao" mà sơ đồ C4
(model/) không diễn đạt được. Đây là tạp phẩm AaC dạng text, versioned cùng code,
review qua PR.

Định dạng: [MADR](https://adr.github.io/madr/) rút gọn (Context → Decision → Consequences).
Đánh số tăng dần, không sửa ADR đã `Accepted` — muốn đổi thì viết ADR mới `Supersedes`.

> **Đây là register canonical (duy nhất) của dự án.** Số ADR thống nhất với các tham chiếu
> "ADR NNNN" trong lịch sử commit. ADR 0001 và 0002 được **back-fill** từ commit history
> (#22, #31, #32) khi hợp nhất register vào repo — quyết định gốc có trước file.

| # | Tiêu đề | Trạng thái | Nguồn |
|---|---|---|---|
| [0001](0001-observability-governance-plane-split.md) | Tách runtime observability khỏi governance (fitness) plane | Accepted | back-fill #22 |
| [0002](0002-msfw-stringidentity.md) | Dùng msfw StringIdentity cho mọi Identity miền | Accepted | back-fill #31, #32 |
| [0003](0003-event-sourced-escrow-ledger.md) | EscrowLedger event-sourced + CQRS read model | Accepted | — |
| [0004](0004-rest-standins-for-sync-integration.md) | REST `/internal/*` thay cho gRPC ở giai đoạn hiện tại | Accepted | — |
| [0005](0005-in-process-consumers-over-separate-workers.md) | Kafka consumer in-process + sự kiện trễ thay cron/worker | Accepted | — |

> Cách dùng với Structurizr: thư mục này được nhúng vào workspace bằng `!adrs adr`
> trong khối `marketplaceSystem` (Structurizr Lite hiển thị tab Decisions).
