# QUY TẮC NỘI DUNG & CÁCH VIẾT TÀI LIỆU KIẾN TRÚC
## (AD/SDD + Tech Spec — viết tay)

| | |
| --- | --- |
| Mã | `STD-DESIGN-DOC-v1.1` |
| Trạng thái | Draft for review |
| Phạm vi | Tài liệu thiết kế cấp hệ thống (**AD/SAD/SDD**) và cấp miền (**Tech Spec**), viết bằng Markdown |
| Neo chuẩn | ISO/IEC/IEEE 42010:2022 · arc42 · C4 (mức trừu tượng) · ADR (Nygard/MADR) · DDD |
| Thay đổi v1.1 | Tái cấu trúc theo review: nêu rõ **chuẩn bootstrapping** + giới hạn (§0.3); thêm trường **`verify:`** cho mọi mệnh đề kiểm-chứng-được (§8); **đóng quan hệ adequacy 42010** (view khai `frames`, DoD phủ concern); truy vết **chuỗi → đồ thị có ngữ nghĩa cạnh** (§6); áp N2/N3 lên chính chuẩn — **AD-Lite + mở rộng theo ngưỡng** (§3); thêm **kịch bản thay đổi/tiến hóa** (§8.3); tách correspondence **logical/physical** + **Context Map kiểu quan hệ chiến lược** (§7); ADR là file riêng + sổ tradeoff (§9); versioning **lát cắt nhất quán** (§10); bổ sung §bảo mật. Bỏ mọi nội dung AaC. |

---

## 0. Mục đích, phạm vi & giới hạn

### 0.1 Mục đích
Để một người **viết được AD/SDD + Tech Spec đúng & đủ bằng Markdown thuần**: tài liệu cần **những mục nào**, mỗi mục **phải chứa nội dung gì**, và **viết thế nào**. Đây là chuẩn về *nội dung và cách viết*, không phải về công cụ.

### 0.2 Vết cắt phạm vi (đọc kỹ — đây là ranh giới thật)
Chuẩn này coi **đầu vào của con người** là trọng tâm:

- **TRONG phạm vi:** *thông tin* cần có + *quy trình do người làm* để tạo và soát nó (khuôn mục, luật viết, kỷ luật "N/A — lý do", vòng đời ADR, checklist DoD, review gate).
- **NGOÀI phạm vi:** *cách mã hóa/biểu diễn bằng công cụ* và *cưỡng chế tự động* (sinh sơ đồ từ một model, kiểm nhất quán/drift bằng máy, validate trong pipeline). Chuẩn này **không** quy định, **không** phụ thuộc chúng.

> Tức ranh giới là **quyết-định-&-quy-trình-của-người** (ở đây) vs **mã-hóa-&-cưỡng-chế-bằng-máy** (ngoài) — *không* phải "thông tin vs ký pháp". Một mệnh đề kiểm-chứng-được (NFR/SLO/invariant) thuộc đây; *phép kiểm tự động* thực thi nó thì ngoài — chuẩn này chỉ giữ **con trỏ** tới phép kiểm (§8).

### 0.3 Giới hạn trung thực — đây là chuẩn *bootstrapping*, không phải trạng-thái-ổn-định
Viết tay đưa bạn tới một **v1.0 sạch & đúng**. Nhưng ở đúng quy mô chuẩn nhắm tới (nhiều BC, nhiều đội, có kiến trúc sư riêng), **một số bảo đảm KHÔNG co giãn theo sức người** và sẽ **xuống cấp theo thời gian** nếu chỉ dựa review tay:

| Bảo đảm | Viết tay v1.0 | Duy trì ở quy mô |
| --- | --- | --- |
| Nhất quán đa-view (§7) | làm được | **không** — cần một nguồn sự thật sinh ra các view |
| Truy vết đầy đủ, không cạnh gãy (§6) | làm được | **không** — cần kiểm tham chiếu tự động |
| Không drift (tài liệu ↔ hiện thực) | đúng tại t0 | **không** — cần đối chiếu máy |

→ Khi chạm các giới hạn này, cần lớp **mã-hóa + cưỡng chế tự động** (ngoài phạm vi chuẩn này). Chuẩn nội dung là **đầu vào** cho lớp đó: nó định nghĩa *thông tin gì cần đúng*, lớp kia lo *giữ cho đúng*.

---

## 1. Nguyên lý nội dung

| # | Nguyên lý | Hệ quả khi viết |
| --- | --- | --- |
| N1 | **Đúng mức trừu tượng cho đúng người đọc** | Mỗi mục/sơ đồ một câu chuyện, một grain; không trộn hệ-thống ↔ nội-bộ-service |
| N2 | **Lõi ổn định, đẩy chi tiết xuống** | AD giữ thứ ít đổi (quyết định, ranh giới, hợp đồng); chi tiết hay đổi → Tech Spec/contract/code |
| N3 | **Chỉ ghi cái bên ngoài phụ thuộc** | Đưa vào tài liệu *bề mặt* người/đội khác dựa vào; cái tự do đổi nội bộ → tầng dưới |
| N4 | **Truy vết được (đồ thị)** | Mọi mục nối được tới mục liên quan bằng ID + ngữ nghĩa cạnh (§6) |
| N5 | **Chưa chốt thì nói thẳng (TBD)** | Không bịa cho "đủ"; `TBD` + nơi theo dõi |

> **Chuẩn áp N2/N3 cho chính nó:** vì thế cấu trúc AD **không** phẳng — có **lõi bất biến (AD-Lite)** mọi tài liệu phải có, và **mở rộng bật theo ngưỡng** (§3). Một bộ khung nặng cố định sẽ hoặc không được viết, hoặc viết một lần rồi rữa.

---

## 2. Hai loại tài liệu

| Loại | Trả lời | Phạm vi | Sở hữu | Vòng đời |
| --- | --- | --- | --- | --- |
| **AD / SDD** | *Vì sao chia & liên kết thế này?* | toàn hệ: ranh giới, quan hệ, hợp đồng, quyết định nặng | Kiến trúc sư / Platform | ổn định |
| **Tech Spec** | *Context này chạy ra sao để hiện thực?* | nội bộ **một** bounded context | Team của BC | đổi thường xuyên |

**1 BC = 1 Tech Spec.** AD **trỏ tới** Tech Spec, **không** sao chép nội dung chi tiết của nó.

---

## 3. Cấu trúc AD/SDD — Lõi (AD-Lite) + Mở rộng theo ngưỡng

> Cột **Tầng**: `Lõi` = mọi AD bắt buộc (AD-Lite); `Mở rộng (trigger)` = chỉ viết khi điều kiện bật. Mục bật mà bỏ → ghi **"N/A — lý do"**. Header luôn có: mã · phiên bản (semver) · trạng thái · ngày · tác giả/duyệt · **lịch sử thay đổi** · mức bảo mật.

| Mục AD | Tầng (trigger) | Phải chứa | Đẩy xuống |
| --- | --- | --- | --- |
| **1. Tổng quan** (mục tiêu+KPI, phạm vi in/out, **stakeholders & concern**, ràng buộc) | **Lõi** | mỗi mục tiêu có KPI đo được; mỗi stakeholder có ≥1 concern (concern phải được gán ở §6) | KPI vận hành chi tiết; giá trị config |
| **2. Kiến trúc tổng thể** (kiểu KT + nguyên tắc; **view cấu trúc đa tầng**; **correspondence logical** = Context Map) | **Lõi** | Context (L1) → Landscape (BC=hộp) → Container/BC (L2); ghi chú grain; mỗi view khai **`frames:`** concern; binding vs indicative (+vòng đời §5) | component nội bộ (L3); runtime per-service (indicative→Tech Spec) |
| **3. Luồng & hành vi** (happy/compensation/async) ở grain hệ thống | **Lõi** | luồng chính + luồng lỗi/saga giữa service↔service | pseudocode; bước nội bộ một service |
| **4. Hợp đồng giao tiếp** (interface/event quan trọng + **bảng bảo đảm tương tác**) | **Lõi** | sync/async, consistency, **idempotency**, ordering, delivery, hành vi lỗi; **trỏ** OpenAPI/AsyncAPI | field/method/mã lỗi đầy đủ |
| **5. Quyết định (ADR Index)** | **Lõi** | index các ADR nặng (id, tiêu đề, trạng thái) — ADR là **file riêng** (§9) | nguyên văn ADR |
| **Glossary** (Ubiquitous Language) | **Lõi** | thuật ngữ + **Bounded Context** + bí danh tránh | — |
| **6. Kiến trúc dữ liệu** | Mở rộng (BC sở hữu dữ liệu bền vững / PII) | quyền sở hữu theo context; reference logic xuyên context; phân loại + retention; bất biến dữ liệu | schema cột; ERD chi tiết; DDL |
| **7. Bảo mật** | Mở rộng (có trust boundary ngoài / tiền / PII / compliance) | trust boundary; authn/authz; mã hóa; **tham chiếu threat model**; data residency; ranh giới secrets; **supply-chain/SBOM**; bảng invariant + **`verify:`** | IAM policy literal |
| **8. Chất lượng, kịch bản & tiến hóa** | Lõi (NFR) + Mở rộng (cây chất lượng khi đa thuộc tính) | NFR đo được + **`verify:`**; kịch bản vận hành + **kịch bản thay đổi/tiến hóa** (§8) | — |
| **9. Lỗi & phục hồi (DR)** | Mở rộng (tier ≥ business-critical) | resilience pattern; RTO/RPO theo tier | runbook |
| **10. Quan sát (Observability)** | Mở rộng (có service runtime) | logging/metrics/tracing/alert ở mức nguyên tắc + chỉ số chính | cấu hình agent |
| **11. Correspondence physical** (BC ⟷ container ⟷ deployment) | Mở rộng (≥ ~vài BC / topology không tầm thường) | bảng nhiều-nhiều (§7) | sizing/replica → IaC/Tech Spec |
| **12. Rủi ro & Nợ kỹ thuật** | **Lõi** | rủi ro/nợ + tác động + biện pháp/theo dõi | — |
| **13. Mục theo miền** | Mở rộng (lĩnh vực đòi hỏi: AI, fintech, y tế…) | nội dung bắt buộc theo miền, hoặc **"N/A — lý do"** | — |
| **Phụ lục** | **Lõi** | A. Tham chiếu + bảng SAD↔Tech Spec · B. (Glossary đã ở Lõi) · C. Checklist · D. Phê duyệt | — |

### Luật viết một số mục (W)
- **W1 — Mục tiêu & NFR phải đo được.** Mỗi NFR: chỉ tiêu (số + đơn vị) · cách đo · nguồn · phạm vi · **`verify:`** (§8). Không đo được = mong muốn, không phải NFR.
- **W2 — View phải khai `frames:`.** Mỗi view (Context/Landscape/Container/Deployment…) liệt kê concern (của §1) mà nó trả lời. (Đóng adequacy 42010 — §6.)
- **W3 — Sơ đồ một grain.** Một sơ đồ = một mức trừu tượng (không trộn BC-hộp với component nội bộ).
- **W4 — Hợp đồng chỉ nêu *đảm bảo*, trỏ đặc tả.** AD ghi capability + bảo đảm tương tác; field đầy đủ ở OpenAPI/AsyncAPI.
- **W5 — Ghi chú grain C4 bắt buộc:** BC = hộp ở **Landscape (≈L1)**; **service + datastore = L2**; **component nội bộ = L3**. "Bên trong BC" ≠ "L3".

---

## 4. Cấu trúc Tech Spec (per Bounded Context)

**Header:** Status · Owner · Reviewers · **Liên kết lên AD** (mục nào) + OpenAPI/AsyncAPI + IaC · Classification (tier + data class) · **Khối "Ranh giới tầng"**: AD giữ **C4 L2** gì · Tech Spec sở hữu **C4 L3** gì · đẩy xuống nữa cái gì.

| Mục | Phải chứa |
| --- | --- |
| 1. Context & Scope | ranh giới BC (vào/ra), trust boundary, goals & non-goals |
| 2. Requirements | FR + NFR/SLO của BC (mỗi cái `verify:`), nối về NFR hệ thống |
| 3. Design overview | 3.1 Module view (tĩnh) · 3.2 C&C (runtime + connector) · 3.3 Deployment per-BC (chi tiết → IaC) |
| 4. Interfaces & data | API (ngữ nghĩa, trỏ OpenAPI) · domain model + **invariant + `verify:`** · data/schema nội bộ · config · dữ liệu cá nhân |
| 5. Key flows | sequence grain C&C: happy / compensation / fail-fast |
| 6. Operations & Resilience (delta) | phần khác/cụ thể hóa so với DR platform |
| 7. Decisions (context-local `ADR-<BC>-N`) + cross-cutting | quyết định nội bộ BC; tham chiếu ADR hệ thống liên quan |
| 8. Test strategy | unit/contract/integration/failure-injection + acceptance |
| 9. Open questions | TBD nội bộ BC |

---

## 5. Luật lọc & phân tầng nội dung (AD ↔ Tech Spec ↔ Contract/Code)

- **L1 — Tier Test:** đổi *chi tiết hiện thực* (đổi field, thêm param optional, đổi framework) mà phải sửa AD → **sai tầng**.
- **L2 — Dependency Test:** chỉ đưa vào AD cái *bên ngoài phụ thuộc* (bề mặt/hợp đồng).
- **L3 — Binding vs Indicative + vòng đời:** công nghệ vào AD chỉ khi **load-bearing**; framework/runtime per-service là **indicative** → Tech Spec. **Vòng đời:** một lựa chọn indicative tích tụ đủ kẻ phụ thuộc có thể âm thầm thành binding → có **trigger tái-phân-loại** (vd ≥k context dựa vào) → viết ADR nâng cấp. Không để kiến trúc ossify trong im lặng.
- **L4 — Field mang sức nặng:** chỉ ghi field *là quyết định* (correlation/tenant/idempotency/version/partition key, field qua ranh giới tin cậy). Còn lại → contract.
- **L5 — TBD tường minh:** đánh dấu + nơi theo dõi.
- **AD dừng ở L2; L3 → Tech Spec** (bắt buộc khi nhiều BC/đội). Deployment: AD grain **BC/zone**; sizing/per-BC → Tech Spec/IaC.

---

## 6. Truy vết = ĐỒ THỊ (không phải chuỗi)

Truy vết là **đồ thị nhiều-nhiều**, không phải chuỗi tuyến tính. Mỗi phần tử có **ID** (Goal/CONCERN/NFR/ADR/VIEW/BC/TechSpec/Test…); mỗi liên kết mang **ngữ nghĩa cạnh**:

| Cạnh | Ý nghĩa |
| --- | --- |
| `satisfies` | NFR/quyết định **đáp** một concern/mục tiêu |
| `refines` | mục dưới **làm mịn** mục trên (NFR ← mục tiêu; Tech Spec ← AD) |
| `constrains` | ràng buộc/ADR **giới hạn** một mục khác |
| `verifies` | test/phép kiểm **chứng minh** một mệnh đề (§8) |
| `supersedes` | ADR **thay thế** ADR cũ |
| `trades-off` | ADR **đánh đổi** giữa các concern đối kháng (§9) |

Quy tắc: một NFR có thể biện minh nhiều ADR; một ADR đáp nhiều NFR; một view phục vụ nhiều concern — **không** giả định 1:1.

**Đóng quan hệ adequacy (42010):** mỗi **concern** thu thập ở mục 1 phải được **frame bởi ≥1 view** (mỗi view khai `frames:` — W2). Concern không view nào frame = lỗ hổng → bổ sung view hoặc loại concern. (Kiểm ở DoD §13.)

---

## 7. Correspondence — tách Logical & Physical

Ranh giới **miền** (BC, không gian vấn đề) và ranh giới **triển khai** (container, không gian giải pháp) *thường* trùng nhưng **không luôn** (một BC có thể là API + worker + projection + nhiều store; một modular monolith host nhiều BC trong một deployable). **Cấm** giả định BC ≅ deployable. Dùng **hai bảng**:

**7.1 Logical — Context Map (kiểu quan hệ chiến lược).** BC ⇄ BC, mỗi cạnh gắn **kiểu DDD**: *Customer/Supplier, Conformist, Anti-Corruption Layer, Open Host Service, Published Language, Shared Kernel, Partnership*. Đây là **hợp đồng chiến lược/tổ chức** (ai conform theo ai, ai hấp thụ thay đổi) — chính là *topo phụ thuộc* mà N3 nói tới. (Khác với *hợp đồng chiến thuật* sync/async/consistency ở mục 4.)

**7.2 Physical — Triển khai (nhiều-nhiều).** BC ⟷ **N container** ⟷ deployment node/zone. Cho phép một BC trải nhiều deployable và một deployable chứa nhiều BC. Mỗi hộp deployment là *node hạ tầng* hoặc *instance của một container đã định nghĩa* — không hộp "lửng".

---

## 8. Mệnh đề kiểm-chứng-được & Kịch bản

### 8.1 Trường `verify:` (bắt buộc, nhất quán)
Mọi **mệnh đề kiểm-chứng-được** — NFR, SLO, **kịch bản chất lượng**, **invariant**, **bảo đảm tương tác** — mang một trường **`verify:`** mô tả *cách kiểm*: `review` (người soát) · `test` (unit/contract/integration) · `monitor` (chỉ số/alert runtime) · `check` (kiểm tự động — chi tiết ngoài phạm vi chuẩn này). Con trỏ thuộc tài liệu; *phép kiểm* thực thi thì ngoài phạm vi (§0.2). Đây là sửa nhất quán: invariant (mục 7) và kịch bản chất lượng (mục 8) cùng loại artifact → cùng có `verify:`.

### 8.2 Kịch bản vận hành (runtime)
Cây chất lượng + kịch bản dạng *stimulus → response đo được*, nối về mục tiêu/NFR; mỗi kịch bản có `verify:`.

### 8.3 Kịch bản thay đổi / tiến hóa (BẮT BUỘC — lý do tồn tại của N2)
"Ổn định" chỉ có nghĩa *tương đối với một phân phối thay đổi kỳ vọng*. AD **phải khai các kịch bản thay đổi được dự đoán** — đây là thứ duy nhất cho phép phán xét ranh giới (BC/L1–L5) đã vẽ đúng chỗ chưa.

| Trường | Ví dụ |
| --- | --- |
| Thay đổi dự kiến | "thay search engine", "thêm loại tenant X", "tách BC này làm đôi", "đổi cổng thanh toán" |
| Mức tác động kỳ vọng | bao nhiêu BC/hợp đồng/ADR bị chạm |
| Ranh giới hấp thụ | mục/ADR/ACL nào *được thiết kế để* cô lập thay đổi này |

Không có kịch bản thay đổi thì L1–L5 trở thành giáo điều không kiểm chứng được.

---

## 9. ADR — format, phân cấp, lưu trữ, tradeoff

- **Format:** Context → Decision → Status → Consequences (Nygard/MADR); trạng thái `Proposed → Accepted → Superseded by …`; **immutable**.
- **File riêng, AD chỉ index.** ADR là **log bất biến tăng vô hạn** → nếu nhét nguyên văn vào AD sẽ phá N2 (lõi ổn định chứa thứ phình mãi). Bắt buộc: mỗi ADR **một file** (`ADR-NNNN-….md` / `ADR-<BC>-N-….md`); AD/Tech Spec chỉ **liệt kê index**.
- **Phân cấp:** **ADR hệ thống** `ADR-NNNN` (liên-BC/cross-cutting, ở AD) vs **ADR context-local** `ADR-<BC>-N` (nội bộ BC, ở Tech Spec, tham chiếu ADR hệ thống). Không trộn dãy số.
- **Sổ tradeoff:** concern xung đột nhau (security↔latency, cost↔availability). Mỗi ADR đánh đổi phải **liên kết tới các concern đối kháng** nó hi sinh (cạnh `trades-off` — §6), để lý lẽ tradeoff không bị mất.

---

## 10. Versioning — semver per-doc + lát cắt nhất quán

- Mỗi tài liệu có **semver + changelog** (bump khi đổi nặng-kiến-trúc).
- Hệ gồm **1 AD + N Tech Spec + nhiều ADR + OpenAPI/AsyncAPI**, version **độc lập** → tham chiếu chéo dễ **dangling**. Bắt buộc khái niệm **"lát cắt nhất quán"**: một **manifest / valid-as-of** ghi bộ phiên bản khớp nhau (AD vX ↔ TechSpec-A vY ↔ contract vZ…), để correspondence (§7) luôn trỏ tới phiên bản đúng.

---

## 11. Anti-patterns (về nội dung)

| # | Anti-pattern | Vì sao sai |
| --- | --- | --- |
| A1 | Gõ tay schema/field/mã lỗi đầy đủ trong AD | Stale tức thì; thuộc OpenAPI/AsyncAPI (W4) |
| A2 | Liệt kê runtime/framework per-service ở AD như quyết định | Indicative, dễ lật (L3) |
| A3 | Lặp cùng thông tin ở nhiều tài liệu | Lệch khi đổi (N3) |
| A4 | Nhồi thiết kế nội bộ (L3) của BC vào AD | Sai tầng; AD phình + đội dẫm đè |
| A5 | Trộn nhiều mức trừu tượng trong một sơ đồ | Khó đọc (N1/W3) |
| A6 | Khung phẳng, không phân Lõi/Mở rộng | Vi phạm N2/N3 cấp meta; nặng → không viết hoặc rữa |
| A7 | Truy vết vẽ như chuỗi 1:1 | Mất thực tế nhiều-nhiều (§6) |
| A8 | Concern thu thập nhưng không view nào frame | Hổng adequacy 42010 (§6) |
| A9 | Mệnh đề kiểm-chứng-được thiếu `verify:` | Đặc tả tách khỏi phép kiểm |
| A10 | Prescribe ranh giới ổn định nhưng không khai kịch bản thay đổi | L1–L5 thành giáo điều (§8.3) |
| A11 | Nhét nguyên văn ADR vào AD | Phá N2 (log phình mãi) (§9) |
| A12 | Bảng correspondence ép BC ≅ deployable | Không biểu diễn BC-đa-deployable / monolith-đa-BC (§7) |
| A13 | Bỏ trống mục không ghi "N/A — lý do" | Mất dấu vết quyết định bỏ qua |

---

## 12. Checklist Definition-of-Done

| # | Hạng mục | ✓ |
| --- | --- | --- |
| 1 | Có đủ **AD-Lite (Lõi)**; mục Mở rộng đúng trigger hoặc "N/A — lý do" | ☐ |
| 2 | Mục tiêu/NFR **đo được**; mỗi mệnh đề kiểm-chứng-được có **`verify:`** | ☐ |
| 3 | **Concern coverage:** mọi concern (mục 1) được ≥1 view `frames` | ☐ |
| 4 | View đa tầng + ghi chú grain (W5); một sơ đồ một grain (W3) | ☐ |
| 5 | Công nghệ phân loại binding/indicative; runtime per-service KHÔNG ở AD; có trigger tái-phân-loại (L3) | ☐ |
| 6 | Hợp đồng: AD trỏ OpenAPI/AsyncAPI; bảng bảo đảm tương tác (mục 4) | ☐ |
| 7 | Correspondence **logical (Context Map + kiểu quan hệ)** và **physical (nhiều-nhiều)** tách bạch (§7) | ☐ |
| 8 | Truy vết **đồ thị** có ID + ngữ nghĩa cạnh (§6) | ☐ |
| 9 | **Kịch bản thay đổi/tiến hóa** đã khai (§8.3) | ☐ |
| 10 | Mỗi BC có 1 Tech Spec đúng khuôn (§4) + khối "Ranh giới tầng"; AD trỏ tới, không sao chép L3 | ☐ |
| 11 | ADR: file riêng + AD index; phân cấp hệ-thống/context-local; ADR tradeoff link concern đối kháng (§9) | ☐ |
| 12 | Rủi ro & Nợ kỹ thuật (mục 12); mọi TBD có nơi theo dõi | ☐ |
| 13 | Glossary có cột Bounded Context | ☐ |
| 14 | Version + changelog + **manifest lát cắt nhất quán** (§10) + phê duyệt | ☐ |

---

## Phụ lục — Ánh xạ chuẩn tham chiếu

| Mục chuẩn này | 42010:2022 | arc42 § | C4 |
| --- | --- | --- | --- |
| Stakeholders/concerns/goals | stakeholder, concern, aspect | 1 | — |
| Constraints | constraint | 2 | — |
| Context & scope | architecture view | 3 | L1 Context |
| Solution strategy | architecture decision | 4 | — |
| Structure views (+ `frames`) | viewpoint, view, model kind, legend, **correspondence** | 5 | Landscape/Container/Component |
| Luồng / hành vi | view (behavioral) | 6 | Sequence |
| Triển khai | view (deployment) | 7 | Deployment (grain BC) |
| Dữ liệu/Bảo mật | aspect, perspective | 8 | — |
| Chất lượng + kịch bản (vận hành & thay đổi) | concern, aspect | 10 | Quality tree + scenarios |
| Quyết định (ADR) | architecture decision, rationale | 9 | — |
| Rủi ro & nợ | concern | 11 | — |
| Glossary | — | 12 | — |
| Truy vết (đồ thị) + adequacy | correspondence, correspondence method | (xuyên suốt) | — |

> Chuẩn này dừng ở **nội dung & quy trình-người**. Việc *mã hóa bằng công cụ* và *cưỡng chế tự động* (giữ nhất quán/truy vết/không-drift ở quy mô — §0.3) là một lớp riêng, áp **sau** và **trên** chuẩn này; nó đổi *cách giữ cho đúng*, không đổi *thông tin gì cần đúng*.
