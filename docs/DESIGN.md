# Tài liệu thiết kế kiến trúc → repo `design-docs`

Tài liệu thiết kế **chính thức** (Architecture Document + Tech Specs + quy tắc viết) cho hệ thống
**Marketplace-AiGen** **không còn lưu trong repo này**. Chúng sống ở repo riêng **`design-docs`** để
tránh trùng lặp / lệch phiên bản (single source of truth).

## Nơi đọc

- **Repo:** [`pmsbkhn/design-docs`](https://github.com/pmsbkhn/design-docs) — đặt cạnh repo này dưới `~/Projects/design-docs`.
- **Architecture Document (AD):** [`design/AD-Marketplace-AiGen.md`](../../design-docs/design/AD-Marketplace-AiGen.md)
- **Tech Specs (per BC):** [`design/techspec/`](../../design-docs/design/techspec/) — Checkout ✅, còn lại 🚧
- **ADR:** [`design/adr/`](../../design-docs/design/adr/)
- **Quy tắc viết (STD):** [`stds/QuyTac-AD-va-TechSpec.md`](../../design-docs/stds/QuyTac-AD-va-TechSpec.md) — **`STD-DOC-v1.17`**

## Vì sao bỏ bản sao trong repo này

Trước đây `docs/design/` + `docs/stds/` là **bản sao nhúng** của design-docs, nhưng đã **lệch** (dừng ở
`STD-DOC-v1.15` + mô hình *"Conform baseline + Delta"* với các file `Baseline-*.md`). Trong khi đó
design-docs đã chuyển sang **`STD-DOC-v1.17` — tài liệu tự chứa** (inline org-default vào AD, bỏ các file
baseline). Để hết drift, repo này **trỏ tới** design-docs thay vì giữ bản sao.

> **Framework hiện thực:** [`msfw`](https://github.com/pmsbkhn/msfw) (`~/Projects/msfw`). Các tài liệu khác
> còn trong `docs/` (SDD, SAD, `aac/`, `tech-spec/`, test notes…) là tài liệu **riêng** của repo này,
> không thuộc design-docs.
