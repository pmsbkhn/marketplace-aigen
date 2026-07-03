# Templates — AD & Tech Spec

Khung mẫu để dev team viết tài liệu kiến trúc **bám sát** `STD-DOC-v1.15`
([../stds/QuyTac-AD-va-TechSpec.md](../stds/QuyTac-AD-va-TechSpec.md)).

| Template | Dùng khi | Cấu trúc |
| --- | --- | --- |
| [AD-Template.md](AD-Template.md) | Viết **1 AD cho cả hệ thống** (quan hệ & ranh giới giữa các BC) | Bám đúng bản mẫu `AD-Marketplace-AiGen.md`: **9 mục (§1–§9) + Phụ lục A–E** |
| [TechSpec-Template.md](TechSpec-Template.md) | Viết **1 Tech Spec cho mỗi BC** (bên trong một BC) | Bám bản mẫu `Checkout.md`: **9 mục (B.1) + checklist** |

> **Baselines:** §6 (Bảo mật), §8 (Phục hồi & DR), §9 (Observability) viết kiểu **"Conform baseline + Delta"** — trỏ tới [Baseline-Security](../stds/Baseline-Security.md) / [Baseline-Resilience-DR](../stds/Baseline-Resilience-DR.md) / [Baseline-Observability](../stds/Baseline-Observability.md) (pin version), chỉ ghi phần đặc thù. Mục **AI Security** đã bỏ khỏi template (chỉ kích hoạt nếu hệ thống thêm thành phần AI/LLM).

## Quy trình

1. Copy template tương ứng sang vị trí thật:
   - AD → `docs/design/AD-<Hệ-thống>.md`
   - Tech Spec → `docs/design/techspec/<BC>.md`
2. Điền theo từng mục — mỗi mục có sẵn 🎯 mục đích · 📐 quy tắc (`R-xx`/`Gxx`) · ✍️ hướng dẫn · 🧩 khung mẫu.
3. Xóa mọi placeholder `«...»` và các khối hướng dẫn (giữ lại nội dung thật).
4. Chạy **checklist Definition-of-Done** ở cuối template trước khi mở PR.

## Bản mẫu tham khảo (đã viết theo chuẩn)

- AD: [../design/AD-Marketplace-AiGen.md](../design/AD-Marketplace-AiGen.md)
- Tech Spec: [../design/techspec/Checkout.md](../design/techspec/Checkout.md)

> **Nguyên tắc vàng (R-0.1):** *"Đổi cái này có buộc BC khác phải biết không?"* → Có = **AD**, Không = **Tech Spec**.
