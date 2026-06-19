# QUY TẮC CẤU TRÚC & NỘI DUNG TÀI LIỆU THIẾT KẾ
## (viết tay / truyền thống — độc lập với cách biểu diễn)

| | |
| --- | --- |
| Mã | `STD-DESIGN-DOC-v1.0` |
| Trạng thái | Draft for review |
| Phạm vi | Tài liệu thiết kế cấp hệ thống (**AD/SAD/SDD**) và cấp miền (**Tech Spec**), viết bằng Markdown |
| Neo chuẩn | ISO/IEC/IEEE 42010:2022 (stakeholder/concern/view) · arc42 (12 phần) · C4 (mức trừu tượng) · ADR (Nygard/MADR) · DDD |
| Quan hệ | Chuẩn này quy định **THÔNG TIN GÌ / ĐẶT Ở ĐÂU**. Cách **BIỂU DIỄN bằng code** (diagrams-as-code, một-model-nhiều-view, Structurizr) và **TỰ ĐỘNG HÓA** (fitness function, CI, drift, model liên-bang) thuộc chuẩn riêng **`STD-AD-AAC`** — áp *sau*, *trên* chuẩn này. |

> **Mục đích:** để một người viết được AD/SDD + Tech Spec **đúng & đủ** bằng Markdown thuần (như các file ví dụ `SDD-MKTPLACE-CORE-v2.2.md`, `tech-spec/TechSpec-Marketplace-*.md`) — *trước khi* nghĩ tới công cụ. Chốt cấu trúc nội dung chuẩn ở đây rồi mới layer AaC lên.
>
> **Ngoài phạm vi (thuộc `STD-AD-AAC`):** diagram phải là code · một model nhiều view · validate/render/drift trong CI · fitness function · bố cục repo/pipeline · model liên-bang (extends). Ở chuẩn này: sơ đồ chỉ cần **đúng nội dung & đúng mức trừu tượng**, vẽ bằng gì (Mermaid/PlantUML/ảnh) là tùy.

---

## 0. Nguyên lý nội dung (5 điều)

| # | Nguyên lý | Hệ quả khi viết |
| --- | --- | --- |
| N1 | **Đúng mức trừu tượng cho đúng người đọc** | Mỗi mục một câu chuyện; không trộn grain (hệ thống ↔ nội bộ một service) |
| N2 | **Lõi ổn định, đẩy chi tiết xuống** | AD giữ thứ ít đổi (quyết định, ranh giới, hợp đồng); chi tiết hay đổi → Tech Spec/contract/code |
| N3 | **Chỉ ghi cái bên ngoài phụ thuộc** | Đưa vào tài liệu cái người/đội khác cần; cái tự do đổi nội bộ → tầng dưới |
| N4 | **Truy vết được** | Mọi mục nối được tới mục nguồn (mục tiêu ↔ NFR ↔ quyết định ↔ view ↔ Tech Spec) |
| N5 | **Chưa chốt thì nói thẳng (TBD)** | Không bịa cho "đủ"; đánh dấu `TBD` + nơi theo dõi |

---

## 1. Hai loại tài liệu & quan hệ

| Loại | Trả lời câu hỏi | Phạm vi | Ai sở hữu | Vòng đời |
| --- | --- | --- | --- | --- |
| **AD / SDD** (cấp hệ thống) | *Vì sao chia & liên kết thế này?* | toàn hệ; ranh giới, quan hệ, hợp đồng, quyết định nặng | Kiến trúc sư / Platform | ổn định |
| **Tech Spec** (cấp miền / 1 BC) | *Context này hoạt động ra sao để hiện thực?* | nội bộ một bounded context | Team của BC đó | đổi thường xuyên |

> **1 Bounded Context = 1 Tech Spec.** AD **trỏ tới** Tech Spec (bảng correspondence), **không** sao chép nội dung chi tiết của nó. Phân định "cái gì ở AD vs Tech Spec" — xem §4.

---

## 2. Cấu trúc tài liệu AD / SDD

> Khuôn mục bắt buộc (theo arc42 + 42010, đã rút gọn cho thực dụng — khớp `SDD-MKTPLACE-CORE-v2.2.md`). Mục không dùng phải ghi **"N/A — lý do"**, không bỏ trống.

**Header:** mã tài liệu · phiên bản (semver) · trạng thái · ngày · tác giả/người duyệt · **bảng lịch sử thay đổi** · mức bảo mật.

| Mục | Phải chứa | KHÔNG chứa (đẩy xuống) |
| --- | --- | --- |
| **1. Tổng quan** | 1.1 Mục tiêu + KPI đo được · 1.2 Phạm vi (in/out scope) · 1.3 Stakeholders & mối quan tâm · 1.4 Giả định & Ràng buộc (kỹ thuật/pháp lý/tổ chức) | KPI vận hành chi tiết; giá trị cấu hình |
| **2. Kiến trúc tổng thể** | 2.1 Kiểu kiến trúc + lý do + nguyên tắc · 2.2 View cấu trúc đa tầng: **Context (L1) → Landscape (BC=hộp) → Container/BC (L2) → Component (L3, chỉ khi cần)** + **ghi chú grain** (BC=Landscape; service/DB=L2; component=L3) + **phân loại công nghệ binding vs indicative** + quy ước nhãn/legend · 2.3 Triển khai (môi trường, topology, deployment ở grain BC/zone) · 2.4 **Bảng correspondence** (Landscape↔Container↔Deployment↔Tech Spec) | Class/hàm nội bộ; thuật toán; runtime/framework từng service (indicative → Tech Spec); YAML/sizing |
| **3. Thành phần** | bảng tổng quan (tên · loại · trách nhiệm 1 dòng) + mỗi thành phần một ô tóm tắt **trỏ tới Tech Spec** | mô tả module/component nội bộ (→ Tech Spec) |
| **4. Luồng dữ liệu** | luồng chính (happy path), luồng bù trừ/saga, luồng bất đồng bộ — **ở grain hệ thống** (service↔service) | pseudocode; log line; bước nội bộ một service |
| **5. Giao diện** | quy ước chung; phân loại interface theo ranh giới tin cậy; danh sách API/event **quan trọng + bảo đảm tương tác** (sync/async, consistency, idempotency, ordering, delivery, lỗi); **trỏ** OpenAPI/AsyncAPI | field/method/mã lỗi đầy đủ (→ OpenAPI/AsyncAPI) |
| **6. Kiến trúc dữ liệu** | quyền sở hữu dữ liệu theo context; ranh giới & reference logic xuyên context; phân loại & retention; bất biến dữ liệu | schema cột; ERD chi tiết; DDL (→ Tech Spec/migration) |
| **7. Bảo mật** | trust boundary; authn/authz; mã hóa; (target vs current nếu theo lộ trình); bảng invariant + nơi enforce (review/tự động) | IAM policy literal; rule cụ thể |
| **8. Chất lượng & mở rộng** | NFR đo được (ISO 25010); SLA/SLO; capacity; chiến lược scaling/caching; **cây chất lượng + kịch bản** (stimulus→response đo được, nối về mục tiêu) | — |
| **9. Lỗi & phục hồi** | phân loại lỗi; resilience pattern (saga/idempotency/DLQ/circuit breaker); DR (RTO/RPO theo tier) | runbook chi tiết |
| **10. Quan sát (Observability)** | logging/metrics/tracing/alerting/dashboard ở mức nguyên tắc + chỉ số chính | cấu hình agent cụ thể |
| **11. ADR Register (hệ thống)** | liệt kê quyết định **nặng-kiến-trúc liên-BC**: id `ADR-NNNN`, tiêu đề, trạng thái, rationale ngắn, view/cơ chế liên quan (xem §7 format) | — |
| **12. Rủi ro & Nợ kỹ thuật** | rủi ro/nợ + tác động + biện pháp/theo dõi | — |
| **13. Mục theo miền** | mục bắt buộc theo lĩnh vực (vd AI Security) hoặc **"N/A — lý do"** | — |
| **Phụ lục** | A. Tham chiếu + **bảng SAD↔Tech Spec** · B. Glossary · C. Checklist trước ban hành · D. Phê duyệt | — |

---

## 3. Cấu trúc tài liệu Tech Spec (per Bounded Context)

> Khuôn mục cố định để các BC nhất quán (khớp `tech-spec/TechSpec-Marketplace-*.md`).

**Header:** Status · Owner · Reviewers · **Liên kết lên AD** (mục nào của SDD) + OpenAPI/AsyncAPI + IaC · Classification (tier + data class) · **Khối "Ranh giới tầng"** (AD giữ C4 L2 gì · Tech Spec sở hữu C4 L3 gì · đẩy xuống nữa cái gì).

| Mục | Phải chứa |
| --- | --- |
| **1. Context & Scope** | ranh giới BC (vào/ra), trust boundary, goals & non-goals |
| **2. Requirements** | FR + NFR/SLO của riêng BC (nối về NFR hệ thống) |
| **3. Design overview** | 3.1 **Module view** (cấu trúc tĩnh — code chia ra sao) · 3.2 **C&C view** (runtime + connector catalog) · 3.3 **Deployment per-BC** (chi tiết: subnet/replica/policy → IaC) |
| **4. Interfaces & data** | API (ngữ nghĩa + mã lỗi quan trọng — **trỏ** OpenAPI/proto) · domain model + invariant · data/schema nội bộ · config & tunables · xử lý dữ liệu cá nhân |
| **5. Key flows** | sequence ở grain C&C: happy path, compensation, fail-fast |
| **6. Operations & Resilience (delta)** | chỉ phần khác/cụ thể hóa so với DR platform |
| **7. Decisions (context-local) + cross-cutting** | `ADR-<BC>-N` (xem §7) + ghi chú cross-cutting (security/observability/tenant) |
| **8. Test strategy** | unit/contract/integration/failure-injection + **fitness/acceptance** |
| **9. Open questions** | TBD nội bộ BC |

---

## 4. Luật lọc & phân tầng nội dung (AD ↔ Tech Spec ↔ Contract/Code)

**Ba câu hỏi, ba tầng:** AD = *vì sao chia & liên kết thế này* · Tech Spec = *context này chạy ra sao* · Code/Contract = *máy làm chính xác gì*.

- **L1 — Tier Test:** đổi một *chi tiết hiện thực* (đổi tên field, thêm param optional, đổi framework) mà phải sửa AD → **sai tầng**. AD chỉ đổi khi một *quyết định kiến trúc* đổi.
- **L2 — Dependency Test:** chỉ đưa vào AD cái *bên ngoài phụ thuộc* (bề mặt/hợp đồng). Cái tự do đổi nội bộ → Tech Spec/code.
- **L3 — Binding vs Indicative:** công nghệ chỉ vào AD khi **load-bearing** (ràng buộc chất lượng/khả mở rộng — event bus, kiểu DB, search engine, WORM). **Framework/runtime từng service là indicative → nêu quyết định *polyglot*, đẩy tên cụ thể xuống Tech Spec.** (Cấm liệt kê runtime per-service ở AD như một quyết định.)
- **L4 — Field mang sức nặng:** chỉ ghi field *là quyết định* (correlation id, tenant id, idempotency key, version, partition key, field qua ranh giới tin cậy). Field còn lại → contract.
- **L5 — TBD tường minh:** thông tin chưa chốt đánh dấu `TBD` + nơi theo dõi (issue/ADR).

**Ghi chú grain C4 (chống nhầm "BC = L2"):** Bounded Context ⇒ hộp ở **Landscape (≈L1)**; **service + datastore ⇒ L2** (AD sở hữu); **component bên trong service ⇒ L3** (Tech Spec sở hữu). "Bên trong BC" ≠ "L3" — BC chứa container (L2) trước, mở container mới tới component (L3).

**Phân tầng theo quy mô:**
- **AD dừng ở L2; L3 → Tech Spec** (mọi quy mô; bắt buộc khi nhiều BC/team).
- Deployment: AD ở grain **BC/zone**; sizing/replica + deployment chi tiết per-BC → Tech Spec/IaC.

> _(Việc các per-BC view có nên RỜI hẳn khỏi file AD trung tâm khi ≥10 BC, và cơ chế "một model nhiều view" để làm điều đó — thuộc `STD-AD-AAC`. Ở chuẩn này chỉ cần: nội dung L3 **đặt trong Tech Spec**, AD **trỏ tới**.)_

---

## 5. Truy vết & Correspondence (nội dung)

- AD phải có **bảng correspondence**: mỗi **BC (Landscape)** ⇄ một dòng **Container** (service+datastore) ⇄ một (cụm) **Deployment** ⇄ một **Tech Spec**. Quan hệ xuyên-BC chỉ khai ở Landscape/Context Map, **không lặp** ở tầng Container.
- Mỗi mục nội dung nối được về nguồn: Mục tiêu → NFR → Quyết định (ADR) → View → Tech Spec → Test.
- Các view của cùng một hệ phải **nhất quán** (không mâu thuẫn nhau). _(Cách phát hiện mâu thuẫn tự động = AaC; ở đây là trách nhiệm review.)_

---

## 6. ADR — định dạng & phân cấp (nội dung)

- **Mọi quyết định nặng-kiến-trúc → một ADR.** Format: **Context → Decision → Status → Consequences** (Nygard) hoặc MADR; trạng thái `Proposed → Accepted → Superseded by …`; immutable (không xóa, viết ADR mới supersede).
- **Phân cấp (bắt buộc khi nhiều BC):**
  - **ADR hệ thống** — `ADR-NNNN`, đánh số toàn cục — quyết định **liên-BC / cross-cutting**; ở **ADR Register của AD** (mục 11).
  - **ADR context-local** — `ADR-<BC>-N` — quyết định **nội bộ một BC**; ở **Tech Spec** của BC, **tham chiếu** ADR hệ thống liên quan.
  - Không trộn hai cấp vào một dãy số.

## 7. Glossary (Ubiquitous Language)

Bảng thuật ngữ bắt buộc ở AD: ID · Thuật ngữ · Định nghĩa thống nhất · **Bounded Context** · bí danh cần tránh · ví dụ. Cột Bounded Context là điểm mấu chốt — cùng một từ mang nghĩa khác ở hai context là tín hiệu ranh giới.

---

## 8. Anti-patterns (cấm — về nội dung)

| # | Anti-pattern | Vì sao sai |
| --- | --- | --- |
| A1 | Gõ tay schema/field/mã lỗi đầy đủ trong AD | Stale tức thì; thuộc OpenAPI/AsyncAPI |
| A2 | Liệt kê tên RPC method / framework per-service ở AD như quyết định | Kéo AD xuống tầng dưới; indicative, dễ lật (L3) |
| A3 | Lặp cùng một thông tin ở nhiều tài liệu | Lệch nhau khi đổi; vi phạm N3/N4 |
| A4 | Nhồi thiết kế nội bộ (L3) của BC vào AD | Sai tầng; AD phình + team dẫm đè |
| A5 | Trộn nhiều mức trừu tượng trong một sơ đồ/mục | Khó đọc; vi phạm N1 |
| A6 | Bỏ trống mục mà không ghi "N/A — lý do" | Mất dấu vết quyết định bỏ qua |
| A7 | Thông tin chưa chốt nhưng viết như đã chốt (không TBD) | Bịa cho "đủ"; vi phạm N5 |
| A8 | ADR viết xong nhưng thiếu Status/Consequences | Không truy vết được vòng đời quyết định |

---

## 9. Checklist Definition-of-Done (cấu trúc & nội dung)

| # | Hạng mục | ✓ |
| --- | --- | --- |
| 1 | AD đủ các mục 1–13 + Phụ lục, hoặc "N/A — lý do" | ☐ |
| 2 | Mục tiêu/NFR **đo được**; cây chất lượng + kịch bản (mục 8) | ☐ |
| 3 | View đa tầng + ghi chú grain (Context/Landscape/Container, Component khi cần) | ☐ |
| 4 | Công nghệ phân loại binding vs indicative; runtime per-service KHÔNG ở AD (L3) | ☐ |
| 5 | Bảng correspondence Landscape↔Container↔Deployment↔Tech Spec (mục 2.4) | ☐ |
| 6 | Hợp đồng: AD chỉ trỏ OpenAPI/AsyncAPI; bảng bảo đảm tương tác (mục 5) | ☐ |
| 7 | Mỗi BC có 1 Tech Spec đúng khuôn (§3); AD trỏ tới, không sao chép L3 | ☐ |
| 8 | Tech Spec có khối "Ranh giới tầng" (AD giữ L2 / Tech Spec giữ L3) | ☐ |
| 9 | ADR đúng format + phân cấp (hệ thống `ADR-NNNN` vs context-local `ADR-<BC>-N`) | ☐ |
| 10 | Rủi ro & Nợ kỹ thuật (mục 12); mọi TBD có nơi theo dõi | ☐ |
| 11 | Glossary có cột Bounded Context | ☐ |
| 12 | Version + changelog + phê duyệt | ☐ |

---

## Phụ lục — Ánh xạ chuẩn tham chiếu

| Mục chuẩn này | 42010:2022 | arc42 § | C4 |
| --- | --- | --- | --- |
| Stakeholders/concerns/goals | stakeholder, concern, aspect | 1 | — |
| Constraints | constraint | 2 | — |
| Context & scope | architecture view | 3 | L1 Context |
| Solution strategy | architecture decision | 4 | — |
| Structure views | viewpoint, view, model kind, legend | 5 | Landscape/Container/Component |
| Runtime / data flows | view (behavioral) | 6 | Sequence |
| Deployment | view (deployment) | 7 | Deployment (grain BC) |
| Data/Security/Cross-cutting | aspect, perspective | 8 | — |
| Quality | concern, aspect | 10 | Quality tree + scenarios |
| Decisions (ADR) | architecture decision, rationale | 9 | — |
| Risks & debt | concern | 11 | — |
| Glossary | — | 12 | — |
| Correspondence/Traceability | correspondence, correspondence method | (xuyên suốt) | — |

> **Bước kế tiếp (sau khi chuẩn này ổn):** áp **`STD-AD-AAC`** để biến phần biểu diễn thành code (một model nhiều view, validate/drift/fitness trong CI, model liên-bang). Chuẩn nội dung này là *đầu vào* cho chuẩn AaC: AaC chỉ đổi **cách biểu diễn & cưỡng chế**, không đổi **thông tin gì cần có**.
