# Chương 7 — ADR & Documentation as Code

Sơ đồ cho biết hệ thống **trông thế nào**, nhưng không nói **tại sao lại thế** và không kể
được văn xuôi dài. Hai mảnh ghép này là **ADR** và **documentation** — cũng là text,
versioned, và nhúng được vào workspace.

## 7.1 ADR — Architecture Decision Record

Mỗi ADR ghi **một quyết định kiến trúc** kèm bối cảnh và hệ quả. Đây là "trí nhớ dài hạn"
của dự án — tránh tranh luận lặp lại "sao hồi đó làm thế này?".

Định dạng phổ biến: **MADR** (Markdown ADR): Context → Decision → Consequences. Đánh số
tăng dần, **không sửa** ADR đã `Accepted` (muốn đổi thì viết ADR mới `Supersedes`).

Ví dụ thật `docs/aac/adr/0004-rest-standins-for-sync-integration.md` (rút gọn):

```markdown
# 4. REST `/internal/*` thay cho gRPC ở giai đoạn hiện tại
- Status: Accepted
- Date: 2026-06-18

## Context
SAD đặc tả S2S bằng gRPC + mTLS. Nhưng dự án ưu tiên chạy được sớm (profile standalone)...

## Decision
Hiện thực biên S2S bằng REST endpoint `/internal/*` đóng vai transport stand-in cho gRPC;
mTLS do Istio mesh đảm nhiệm ở tầng hạ tầng.

## Consequences
- (+) Chạy/test được ngay; đổi sang gRPC sau chỉ ảnh hưởng adapter (hexagonal).
- (−) Chưa có hợp đồng schema cưỡng chế cho REST S2S.
```

Register canonical của dự án có 5 ADR (số thống nhất với tham chiếu "ADR NNNN" trong lịch
sử commit; 0001–0002 là quyết định nền back-fill từ history):

| ADR | Quyết định |
|---|---|
| 0001 | Tách **runtime observability** khỏi **governance (fitness) plane** |
| 0002 | Dùng **msfw StringIdentity** cho mọi Identity miền |
| 0003 | EscrowLedger **event-sourced** + CQRS read model (chỉ nơi cần audit tài chính) |
| 0004 | **REST `/internal/*`** thay gRPC ở giai đoạn này |
| 0005 | **Consumer in-process** + sự kiện trễ, thay cho worker/cron tách riêng |

> Mẹo: ADR nên link tới view liên quan ("xem `PaymentComponents`") để người đọc nhảy từ
> "tại sao" sang "trông thế nào".

## 7.2 Nhúng ADR vào workspace: `!adrs`

Structurizr đọc cả thư mục ADR và hiển thị thành tab **Decisions**:

```dsl
// trong marketplaceSystem { } của workspace.dsl
!adrs adr
```

Đường dẫn `adr` tương đối với file chứa directive (`workspace.dsl`). Khi render bằng
Structurizr Lite, ADR hiện cạnh sơ đồ — quyết định và cấu trúc ở cùng một nơi.

## 7.3 Documentation as Code: `!docs`

Văn xuôi (giới thiệu, hướng dẫn, runbook…) cũng là markdown trong repo, nhúng bằng `!docs`:

```dsl
// trong marketplaceSystem { } của workspace.dsl
!docs documentation
```

Structurizr đọc các file trong `documentation/` theo thứ tự tên file, render thành tab
**Documentation**. Dự án có 5 mục: introduction, containers, runtime, deployment, glossary.

## 7.4 Embed sơ đồ vào văn xuôi

Điểm mạnh nhất: tài liệu **nhúng sơ đồ sống** bằng `![](embed:<view-key>)` — sơ đồ luôn là
bản mới nhất, không phải ảnh chụp cũ. Trích `docs/aac/documentation/03-runtime.md`:

```markdown
## Đặt hàng (happy path)
Checkout điều phối đồng bộ theo thứ tự price → reserve → split → order → escrow...

![Checkout Saga](embed:CheckoutSaga)
```

`embed:CheckoutSaga` trỏ tới view `key` đã định nghĩa ở `views/integration.dsl`. CI sẽ bắt
nếu key sai (Chương 8).

## 7.5 Glossary & ubiquitous language

Một mục tài liệu đặc biệt giá trị: **glossary** — ngôn ngữ chung giữa code, sơ đồ và
nghiệp vụ (khái niệm DDD). Trích `docs/aac/documentation/05-glossary.md`:

```markdown
| Thuật ngữ | Nghĩa |
|---|---|
| Escrow | Quỹ giữ tiền trung gian: hold tiền buyer, release cho merchant khi đơn xong. |
| Outbox | Bảng ghi sự kiện cùng transaction nghiệp vụ; relay đẩy ra Kafka. |
| Saga / Compensation | Giao dịch dài nhiều service; lỗi thì chạy bù trừ ngược thứ tự. |
```

Kèm bảng **state machine** mỗi aggregate và **danh mục sự kiện** Kafka. Đây là viewpoint
"từ vựng" cho stakeholder business/QA — đôi khi quý hơn cả sơ đồ.

---

➡️ Chương 8: ghép tất cả vào **vòng đời** — as-built vs target, hệ sinh thái tạp phẩm, và
**CI** chống trôi tài liệu.
