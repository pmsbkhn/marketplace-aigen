# Chương 9 — Quy trình & Governance: giữ code và AaC docs không lệch nhau

Chương 8 nói "cập nhật tài liệu cùng PR với code". Nhưng *kỷ luật con người* sẽ trôi. Chương
này biến nó thành **quy trình có vai trò rõ ràng + cơ chế cưỡng chế bằng máy**.

## 9.1 Sự thật phũ phàng: `validate`/`export` KHÔNG kiểm docs khớp code

Phải nói thẳng để không ảo tưởng:

> CI `aac.yml` (Chương 8) chỉ kiểm **AaC docs tự nhất quán** (cú pháp, identifier, view,
> embed). Nó **KHÔNG biết** code thật có đúng như sơ đồ hay không. Bạn có thể xoá một
> use-case trong code mà sơ đồ vẫn xanh.

Vậy điều gì *thật sự* ràng buộc code với kiến trúc? Câu trả lời: **không phải một thứ, mà
một lớp phòng thủ nhiều tầng** — phần lớn dự án này đã có sẵn.

## 9.2 Các actor (role) tham gia

| Role | Sửa gì trong AaC | Trách nhiệm |
|---|---|---|
| **Dev (service owner)** | `model/<context-của-mình>.dsl`, ADR nhỏ | Cập nhật component/quan hệ khi đổi code service mình |
| **Tech Lead / Architect** | `workspace.dsl` (quan hệ liên-service), `styles.dsl`, ADR lớn, ranh giới context | Người **duyệt bắt buộc** cho thay đổi cấu trúc & quan hệ liên-service |
| **Product Owner** | (đọc) `SystemContext`, glossary | Xác nhận ngữ nghĩa nghiệp vụ, từ vựng |
| **SRE / Ops** | `model/deployment.dsl`, `views/operations.dsl` | Sở hữu deployment view, khớp với `deploy/` |
| **Security** | tag `Ingress/Sensitive/...`, `views/security.dsl`, ADR bảo mật | Duyệt thay đổi trust boundary / dữ liệu nhạy cảm |
| **QA / Governance** | fitness functions | Bảo trì luật kiến trúc chạy được |

RACI gọn cho một thay đổi kiến trúc điển hình:

| Hoạt động | Dev | Architect | SRE | Security |
|---|---|---|---|---|
| Sửa model service mình | **R** | C | I | I |
| Sửa quan hệ liên-service / ranh giới | C | **R/A** | I | C |
| Sửa deployment view | I | C | **R/A** | I |
| Sửa tag/ view bảo mật | I | C | I | **R/A** |
| Viết ADR | R | **A** | C | C |

(R=làm, A=phê duyệt cuối, C=hỏi ý, I=thông báo)

## 9.3 Cơ chế RÀNG BUỘC code ↔ docs (lớp phòng thủ)

Đây là trái tim câu hỏi. Năm tầng, ghi rõ **tự động hay người**, và **ràng buộc cái gì**:

| # | Cơ chế | Ràng buộc điều gì | Auto? | Trong repo |
|---|---|---|---|---|
| 1 | **Fitness functions (ArchUnit)** | Code **tuân** luật cấu trúc mà C4 khẳng định (domain ⊥ adapter, layering, naming `…Uc/…Oa/…Controller`, vị trí package) | ✅ build fail | `*/adapter/src/test/.../architecture/FitnessFunctionsTest.java` |
| 2 | **Contract tests** | Cạnh Kafka trong model khớp **producer/consumer thật** (schema event) | ✅ build fail | `*ContractTest` + `*ContractBindingTest`, `/contracts/*.json` |
| 3 | **Structurizr validate/export** | AaC docs tự nhất quán (cú pháp/identifier/view/embed) | ✅ CI | `.github/workflows/aac.yml` |
| 4 | **CODEOWNERS + branch protection** | Đúng role duyệt đúng vùng (architect duyệt `docs/aac`) | ✅ chặn merge | (đề xuất §9.5) |
| 5 | **Same-PR rule + review checklist** | Đổi code thì đổi docs cùng lúc; người soát "sơ đồ có khớp diff?" | 🧑 người | Checklist Ch.10 |

Điểm mấu chốt cần thấm:

> **Tầng 1–2 là chỗ code thật bị "trói" vào kiến trúc.** Sơ đồ C4 nói "domain không gọi
> adapter", "Catalog publish ProductCreated" — *fitness function* và *contract test* biến
> các phát biểu đó thành test; code phá luật là **build đỏ**, không merge được. Sơ đồ và
> luật chia sẻ cùng một sự thật cấu trúc.
> Tầng 3 giữ docs sạch. Tầng 4–5 đảm bảo *con người và quy trình* khép vòng cho phần mà
> máy chưa kiểm được (vd 1:1 "mỗi component ↔ một class").

## 9.4 Ràng buộc xảy ra ở ĐÂU trong vòng đời?

Câu hỏi của bạn — "có lẽ ở bước commit của đội dev?" — gần đúng. Chính xác là ở **nhiều
chốt**:

```
Dev sửa CODE
   │  (local) mvn test  ──▶ fitness + contract test chạy NGAY tại máy dev  ← chốt 1 (sớm nhất)
   │  cập nhật AaC docs trong CÙNG branch
   ▼
git commit  ──▶ (tuỳ chọn) pre-commit/pre-push hook chạy lại test       ← chốt 2
   ▼
mở Pull Request
   │  CI fitness.yml : build + test 6 service + fitness verdicts        ← chốt 3 (cưỡng chế)
   │  CI aac.yml     : validate + export AaC docs                        ← chốt 4
   │  CODEOWNERS     : auto-request Architect/Security review            ← chốt 5
   ▼
Review (theo RACI §9.2) + checklist "sơ đồ khớp diff?"                    ← chốt 6 (người)
   ▼
merge ▶ render/publish (Structurizr Lite)
```

- **Chốt 1/3 (fitness)**: nếu dev đổi code phá kiến trúc → đỏ ngay, kể cả sơ đồ chưa đụng.
- **Chốt 3 (contract)**: nếu đổi event mà quên cập nhật consumer/fixture → đỏ.
- **Chốt 4 (aac)**: nếu sơ đồ hỏng/đổi tên gãy → đỏ.
- **Chốt 5–6 (người)**: bắt phần "sơ đồ vẫn vẽ component đã bị xoá khỏi code" — máy chưa
  bắt được, người soát + checklist bắt.

## 9.5 Governance bằng file (đề xuất thêm vào repo)

Để chốt 5 thành **cưỡng chế** chứ không "tuỳ tâm", thêm `.github/CODEOWNERS`:

```
# Thay đổi AaC docs cần Architect duyệt
/docs/aac/                     @org/architects
/docs/aac/views/security.dsl   @org/security
/docs/aac/model/deployment.dsl @org/sre
# Mỗi context do team sở hữu
/docs/aac/model/payment-context.dsl @org/team-payment
```

Kèm **branch protection** trên `main`: bắt buộc PR + pass check `aac.yml` & `fitness.yml` +
≥1 review từ CODEOWNERS. Khi đó "đúng người duyệt đúng vùng" là luật, không phải lời nhắc.

## 9.6 Giới hạn còn lại (trung thực)

Máy **chưa** tự kiểm "mọi component trong model tồn tại 1:1 trong code". Ba cách thu hẹp:

1. **Tên khớp code** (Ch.10 best practice #10) → reviewer dễ phát hiện lệch.
2. **Component nhỏ, ổn định** (theo lớp hexagonal, không theo từng class) → ít phải đổi.
3. (Nâng cao) Structurizr có **component finder** quét annotation/reflection để *sinh*
   component từ code — có thể thêm sau để tự đồng bộ L3. Hiện dự án ưu tiên model viết tay
   cho rõ ràng; ghi đây như hướng tiến hoá.

> Quy tắc sống còn, nhắc lại: **đổi code → đổi model/ADR trong CÙNG PR.** Quy trình ở trên
> chỉ làm quy tắc này *khó vi phạm*, không thay thế ý thức.

## 9.7 Tóm tắt một câu

Code bị ràng buộc với AaC docs **không phải bằng một bước commit thần kỳ**, mà bằng: *fitness
function + contract test* (trói code vào luật cấu trúc, build đỏ nếu lệch) **+** *CI validate
docs* **+** *CODEOWNERS/branch protection* (đúng người duyệt) **+** *same-PR rule* (đổi cùng
lúc). Năm tầng đó khép thành vòng đời không-trôi.

---

➡️ Chương 10: đúc kết best practices, anti-pattern và bài tập thực hành.
