# Architecture Decision Records (ADR)

Mỗi ADR ghi lại **một quyết định kiến trúc** và lý do — phần "tại sao" mà sơ đồ C4
(model/) không diễn đạt được. Đây là tạp phẩm AaC dạng text, versioned cùng code,
review qua PR.

Định dạng: [MADR](https://adr.github.io/madr/) rút gọn (Context → Decision → Consequences).
Đánh số tăng dần, không sửa ADR đã `Accepted` — muốn đổi thì viết ADR mới `Supersedes`.

| # | Tiêu đề | Trạng thái |
|---|---|---|
| [0001](0001-event-sourced-escrow-ledger.md) | EscrowLedger event-sourced + CQRS read model | Accepted |
| [0002](0002-rest-standins-for-sync-integration.md) | REST `/internal/*` thay cho gRPC ở giai đoạn hiện tại | Accepted |
| [0003](0003-in-process-consumers-over-separate-workers.md) | Kafka consumer in-process + sự kiện trễ thay cron/worker | Accepted |

> Cách dùng với Structurizr: thư mục này có thể nhúng vào workspace bằng `!adrs adr`
> trong khối `views`/`workspace` để render kèm sơ đồ (xem README chính của docs/aac).
