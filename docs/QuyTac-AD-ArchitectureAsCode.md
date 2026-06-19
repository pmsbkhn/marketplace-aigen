# QUY TẮC NỘI DUNG & BIỂU DIỄN CHO TÀI LIỆU KIẾN TRÚC (AD)
## theo tinh thần Architecture-as-Code (AaC)

| | |
| --- | --- |
| Mã | `STD-AD-AAC-v1.1` |
| Trạng thái | Draft for review |
| Thay đổi v1.1 | Thêm **PHẦN I — Quy mô & Phân tầng (hệ lớn)**: định nghĩa grain C4 (R-I1), AD dừng ở L2 / L3→Tech Spec (R-I2), nội dung Tech Spec (R-I3), AD phân tầng trên model liên-bang (R-I4), grain Deployment khi scale (R-I5), ADR phân cấp hệ-thống vs context-local (R-I6), ngưỡng & lộ trình (R-I7). Mở rộng R-B8; bổ sung checklist + ví dụ fitness. |
| Phạm vi | Mọi tài liệu kiến trúc (SAD/SDD/AD) cấp hệ thống & cấp miền |
| Neo chuẩn | ISO/IEC/IEEE 42010:2022 · arc42 · C4 model / Structurizr · ADR (Nygard/MADR) · Fitness Functions (Building Evolutionary Architectures) · Docs-as-Code · OpenAPI/AsyncAPI · DDD Context Mapping |

> **Cách đọc:** mỗi quy tắc có mã `R#`. Cột *Enforce* cho biết kiểm bằng **người review** (gate) hay **máy** (fitness function/CI). Triết lý xuyên suốt: tài liệu kiến trúc là **artifact dạng văn bản, version-controlled, sinh ra từ một model duy nhất, và được CI kiểm tra** — không phải file vẽ tay tĩnh.

---

## 0. Mục đích & nguyên lý nền

### 0.1 AaC là gì (định nghĩa làm việc)

Architecture-as-Code = mô tả kiến trúc (model, view, quyết định, hợp đồng, ràng buộc chất lượng) bằng **văn bản có cấu trúc**, đặt trong **version control**, **sinh view từ một nguồn duy nhất**, và **xác minh tự động trong pipeline**. Đây là sự hợp lưu của bốn dòng thực hành: *diagrams/models-as-code* (Structurizr/C4), *docs-as-code*, *decisions-as-records* (ADR), và *governance-as-code* (fitness functions).

### 0.2 Bảy nguyên lý (mọi quy tắc bên dưới phục vụ các nguyên lý này)

| # | Nguyên lý | Hệ quả |
| --- | --- | --- |
| P1 | **Single Source of Truth** — một model, nhiều view | Không vẽ lặp; view là *hình chiếu* của model |
| P2 | **Text & version-controlled** | Diff được, review qua PR, có lịch sử/blame |
| P3 | **Separation: model ↔ view ↔ render** | Đổi cách trình bày không đụng nội dung; tránh lock-in công cụ |
| P4 | **Right abstraction per audience** | Mỗi view một mức trừu tượng, một câu chuyện |
| P5 | **Correspondence/Traceability** | Mọi view nhất quán & truy vết được lẫn nhau và xuống tầng dưới |
| P6 | **Decisions are first-class & enforced** | Quyết định ghi thành ADR + có cơ chế *assure* |
| P7 | **Stable core, push detail down** | AD giữ thứ ổn định; chi tiết hay đổi đẩy xuống Tech Spec/code/contract |

---

## 1. Khung chuẩn tham chiếu (vì sao các quy tắc có hình dạng như vậy)

**ISO/IEC/IEEE 42010:2022** là chuẩn quốc tế cho *architecture description*. Bản 2022 quy định một AD nên chứa các *AD element*: stakeholder, concern, aspect, stakeholder perspective, architecture viewpoint, architecture view, model kind, legend, view component, architecture decision, architecture rationale và các *correspondence* giữa chúng. Bản mới tổng quát hóa chủ thể từ "System of Interest" thành "Entity of Interest", và đổi "Architecture Model" thành "View Component". Điểm cốt lõi cho bộ quy tắc này: chuẩn nhấn mạnh sự nhất quán giữa các thành phần — các view và model phải nhất quán với nhau, đảm bảo bằng *correspondence rules*; và model kind được nêu như một trường hợp tuân thủ mới nhằm khuyến khích kiến trúc dựa-trên-model — chính là chỗ AaC gắn vào.

**arc42** cho bộ khung 12 phần đã được kiểm nghiệm để trả lời "ghi gì / ghi thế nào": giới thiệu & mục tiêu; ràng buộc; bối cảnh & phạm vi; chiến lược giải pháp; building block view; runtime view; deployment view; cross-cutting concepts; quyết định kiến trúc; chất lượng (cây + kịch bản); rủi ro & nợ kỹ thuật; thuật ngữ. arc42 cố ý để mọi phần là tùy chọn — giống một tủ nhiều ngăn vẫn có giá trị dù vài ngăn để trống.

**C4 / Structurizr** là hiện thân AaC cho phần view. Structurizr là công cụ "models as code" cho C4: viết DSL để sinh nhiều sơ đồ từ một model duy nhất, thân thiện version control và dễ tích hợp pipeline CI/CD, và nhờ là văn bản nên cho phép AI/tooling phân tích model, tóm tắt, truy vấn và phát hiện architectural drift. Việc tách *authoring* khỏi *rendering* khiến giảm lock-in vào một công cụ vẽ và cho phép cắt model thành nhiều view rồi render bằng công cụ phù hợp nhất (export PlantUML/Mermaid/PNG/SVG).

**ADR + Fitness functions** là cặp quyết định–thực thi. Fitness function, do Neal Ford, Rebecca Parsons và Patrick Kua giới thiệu trong Building Evolutionary Architectures, là các kiểm thử tự động xác minh thuộc tính kiến trúc trên mỗi build hoặc deploy. Quan hệ với ADR: decision record ghi lại quyết định, còn fitness function bảo chứng (assure) quyết định đó; bởi một quyết định viết ra mà không được thực thi chỉ là "documentation theater". Vận hành: lưu ADR trong version control (vd thư mục /docs/adr) để tham chiếu được từ CI pipeline.

---

## PHẦN A — QUY TẮC NỘI DUNG (ghi *cái gì* vào AD)

> Mỗi khối nội dung gắn với *AD element* của 42010 và *section* của arc42. Cột **Đẩy xuống** nêu cái KHÔNG thuộc AD.

### A.1 Bảng nội dung bắt buộc

| Mã | Khối nội dung (bắt buộc) | 42010 element | arc42 § | Đẩy xuống (KHÔNG ở AD) |
| --- | --- | --- | --- | --- |
| R-A1 | **Stakeholders & concerns** — ai đọc, quan tâm gì | stakeholder, concern | 1 | Danh sách liên hệ nhân sự |
| R-A2 | **Mục tiêu & quality goals** (đo được) | concern, aspect | 1.2 | KPI vận hành chi tiết |
| R-A3 | **Ràng buộc** (kỹ thuật, tổ chức, pháp lý) | constraint | 2 | Giá trị cấu hình cụ thể |
| R-A4 | **Bối cảnh & phạm vi** — biên hệ thống, đối tác ngoài, in/out scope | architecture view (context) | 3 | Chi tiết payload từng API |
| R-A5 | **Chiến lược giải pháp** — kiểu kiến trúc & lý do | architecture decision (tổng) | 4 | — |
| R-A6 | **Building-block / structure views** đa tầng (Landscape → Container → Component khi cần) | viewpoint, view, view component, model kind | 5 | Class/function nội bộ, thuật toán |
| R-A7 | **Runtime / behavior views** — các kịch bản chính & saga/compensation | view (behavioral) | 6 | Pseudocode, log line cụ thể |
| R-A8 | **Deployment view** — node hạ tầng, ánh xạ container→node | view (deployment) | 7 | YAML/IaC, sizing số cụ thể |
| R-A9 | **Data architecture** — quyền sở hữu dữ liệu, ranh giới, reference logic, phân loại/retention, bất biến | view, aspect | 8 | Schema cột, ERD chi tiết, DDL |
| R-A10 | **Security architecture** — trust boundary, authn/authz, mã hóa, ZTA target vs current | aspect, perspective | 8 | IAM policy literal, rule cụ thể |
| R-A11 | **Cross-cutting concepts** — idempotency, tracing, error model, i18n… | aspect | 8 | Thư viện/middleware cụ thể |
| R-A12 | **Quality requirements** — quality tree + kịch bản (link mục tiêu) | concern, aspect | 10 | — |
| R-A13 | **Architecture decisions (ADR)** — quyết định nặng-kiến-trúc + rationale | architecture decision, rationale | 9 | — |
| R-A14 | **Risks & technical debt** — rủi ro/nợ + biện pháp | concern | 11 | — |
| R-A15 | **Hợp đồng giao tiếp (tham chiếu)** — interface/event ở mức *capability + đảm bảo*; trỏ tới OpenAPI/AsyncAPI | view component, correspondence | 3/5 | Field/method/mã lỗi đầy đủ |
| R-A16 | **Correspondence/Traceability** — bảng ánh xạ giữa các view & xuống Tech Spec | correspondence, correspondence method | (xuyên suốt) | — |
| R-A17 | **Glossary** — thuật ngữ nghiệp vụ & kỹ thuật | — | 12 | — |

### A.2 Quy tắc lọc nội dung (tiêu chí "có thuộc AD không")

- **R-A18 — Phép thử tầng (Tier Test):** nếu đổi một *chi tiết hiện thực* (đổi tên field, thêm param optional, đổi framework) mà buộc sửa AD → chi tiết đó **sai tầng**. AD chỉ đổi khi một *quyết định kiến trúc* đổi.
- **R-A19 — Phép thử phụ thuộc:** chỉ đưa vào AD những gì *bên ngoài phụ thuộc vào* (bề mặt/hợp đồng). Cái bạn được tự do đổi mà không ai bên ngoài chịu ảnh hưởng → Tech Spec/code.
- **R-A20 — Công nghệ binding vs indicative:** công nghệ chỉ vào AD khi *load-bearing* (ràng buộc chất lượng/khả mở rộng — vd event bus, kiểu DB, search engine, WORM). Framework/runtime từng service là *indicative* → nêu quyết định *polyglot* thay vì liệt kê, đẩy tên cụ thể xuống Tech Spec.
- **R-A21 — Field mang sức nặng kiến trúc:** chỉ ghi field *là quyết định* (correlation/causation id, tenant id, idempotency key, version, partition key, field qua ranh giới tin cậy). Field còn lại → contract artifact.
- **R-A22 — TBD tường minh:** thông tin chưa chốt phải đánh dấu `TBD` + gắn ADR/issue theo dõi, không bịa cho "đủ".

---

## PHẦN B — QUY TẮC BIỂU DIỄN (ghi *thế nào* theo AaC)

### B.1 Model & View

- **R-B1 — One model, many views (P1):** mọi sơ đồ structure phải là *view* sinh từ một model nguồn (vd `workspace.dsl`). Cấm vẽ tay trùng lặp cùng thông tin ở nhiều nơi.
- **R-B2 — Diagrams-as-code:** sơ đồ lưu dạng văn bản (Structurizr DSL ưu tiên cho C4; Mermaid/PlantUML cho phần còn lại), **không** nhúng ảnh nhị phân không-nguồn. Ảnh render là *output*, không phải *source*.
- **R-B3 — Tách authoring/rendering (P3):** chọn định dạng cho phép export đa đích (PlantUML/Mermaid/SVG) để tránh lock-in.
- **R-B4 — Một sơ đồ, một câu chuyện, một mức trừu tượng (P4):** không trộn grain. Nếu một sơ đồ "tất-cả-trong-một" bắt đầu rối → tách theo bounded context/use case.

### B.2 Tầng trừu tượng (C4 + DDD), cho hệ lớn

- **R-B5 — Zoom đa tầng:** dùng tối thiểu **System Landscape → Container (per bounded context) → Component (chỉ khi cần)**. Mức C4 *Code* không duy trì trong model — sinh từ IDE/tooling khi cần.
- **R-B6 — Bounded context là hộp ở tầng cao:** ở Landscape, mỗi context là **một hộp**; service/datastore chỉ lộ ở Container diagram của chính context đó.
- **R-B7 — Datastore là container:** mỗi datastore *nằm trong* hộp context sở hữu; cấm để database trôi nổi ngoài context (giữ grain nhất quán; polyglot không làm rối Landscape).
- **R-B8 — Ngưỡng tách:** ≤ ~7 context có thể giữ một Container view; ~10+ context hoặc đa P&L → tách Container-per-context **và** chuyển hẳn sang model-as-code. _(Chi tiết quy mô lớn: **PHẦN I**.)_
- **R-B9 — DDD Context Map đi kèm Landscape:** Landscape cho *topology*; Context Map cho *ngữ nghĩa quan hệ* (Customer–Supplier, Conformist, ACL, OHS, Published Language, Shared Kernel).

### B.3 Nhãn, legend, quy ước tên

- **R-B10 — Nhãn quan hệ = ý định (+ protocol ở C4 L2):** mô tả *mục đích* ("lấy giá", "giữ tồn kho") + protocol ("gRPC"/"HTTPS"/nét đứt cho event). **Cấm** liệt kê tên RPC method trên sơ đồ kiến trúc (chúng thuộc 5.1.x/proto, dễ stale). Tên *event* được giữ vì là Published Language.
- **R-B11 — Legend bắt buộc (42010 *legend*):** mọi view có chú thích phân biệt loại phần tử (service / datastore / external / boundary / node hạ tầng).
- **R-B12 — Quy ước tên thống nhất:** "X BC" ở tầng cấu trúc, "X Service" ở runtime/flow/deployment. Không mặc định "1 context = 1 service"; tài liệu phải tránh ngầm khẳng định điều đó.

### B.4 Correspondence / Traceability (P5, 42010)

- **R-B13 — Mỗi hộp khai báo loại của nó & ánh xạ tầng kề:** mọi phần tử ở deployment phải là *node hạ tầng* HOẶC *instance của một container đã định nghĩa*; không có hộp "lửng".
- **R-B14 — Bảng correspondence:** AD phải có bảng ánh xạ Landscape ↔ Container ↔ Deployment, và SAD ↔ Tech Spec. Mâu thuẫn giữa view là lỗi chặn (xem fitness function R-E4).

### B.5 Versioning của tài liệu

- **R-B15 — Semver cho AD + changelog:** mỗi thay đổi nặng-kiến-trúc bump version, ghi changelog, gắn ADR.
- **R-B16 — Sống cạnh code:** AD + ADR + workspace model đặt trong repo (vd `/docs/architecture`), review qua PR như code.

---

## PHẦN C — QUY TẮC TẦNG & HỢP ĐỒNG (AD ↔ Tech Spec ↔ Code)

- **R-C1 — Ba tầng, ba câu hỏi:** AD = *tại sao chia & liên kết thế này*; Tech Spec = *context này hoạt động ra sao để hiện thực*; Code = *máy làm chính xác gì*. Vòng đời ổn định giảm dần.
- **R-C2 — Hợp đồng đồng bộ:** AD nêu interface ở mức capability + đảm bảo; **đặc tả đầy đủ → OpenAPI/proto** (validated trong CI), AD *trỏ tới* chứ không *sao chép*.
- **R-C3 — Hợp đồng bất đồng bộ là hạng nhất:** event có artifact-nhà riêng (**AsyncAPI + schema registry**) + chính sách tiến hóa schema (compatibility mode, breaking → version/topic mới). Cấm coi event là "phụ phẩm" của publisher.
- **R-C4 — Bảng "đảm bảo tương tác":** mỗi interface/event khai báo sync/async, consistency, idempotency, ordering, delivery, hành vi lỗi/suy giảm — đây là phần "chống thay đổi chi tiết", thuộc AD.
- **R-C5 — Hợp đồng liên-tổ-chức tách riêng:** API dùng chung giữa hai bounded context/đơn vị nên có "Interface/API Contract" do cả provider–consumer đồng sở hữu, không chôn trong Tech Spec nội bộ một bên.

---

## PHẦN I — QUY TẮC QUY MÔ & PHÂN TẦNG (HỆ LỚN, > ~10 BC)

> Phần A–C viết cho một hệ cỡ vừa / một AD nguyên khối. Khi hệ lớn (nhiều bounded context,
> nhiều team, đa P&L), một AD nguyên khối với sơ đồ của mọi BC sẽ rối + nhiều team dẫm đè một
> file. Phần này chốt cách **phân tầng AD và đẩy chi tiết xuống Tech Spec** một cách có luật.

- **R-I1 — Định nghĩa grain C4 (chống nhầm "BC = L2"):** các mức C4 là **L1 System → L2 Container → L3 Component**; *Container* C4 = một đơn vị chạy độc lập = **một service HOẶC một datastore**. Ánh xạ chuẩn:
  - **Bounded Context** ⇒ vẽ là **một hộp ở System Landscape (≈ L1)** — *trên* L2 một bậc.
  - **service + database** của BC ⇒ **L2** (AD sở hữu).
  - **component bên trong một service** (controller / use-case / aggregate / adapter) ⇒ **L3** (Tech Spec sở hữu).
  - "Bên trong BC" KHÔNG đồng nghĩa "L3": một BC chứa *container (L2)* trước; chỉ khi mở *một container* mới tới *component (L3)*.

- **R-I2 — AD dừng ở L2; L3 → Tech Spec.** AD chỉ giữ tới **L2** cho mỗi BC (service + datastore + ranh giới + quan hệ + hợp đồng). **Component (L3) — cấu trúc nội bộ một service — KHÔNG đặt trong AD**, mà thuộc **Tech Spec của BC đó**. Căn cứ: Tier Test (R-A18) + Dependency Test (R-A19) + P7. Ngoại lệ duy nhất: một component *load-bearing xuyên BC* (vd pattern dùng lại nhiều context) — nêu ở AD như *quyết định*, không vẽ nội bộ.

- **R-I3 — 1 BC = 1 Tech Spec, team sở hữu, chứa L3.** Mỗi BC có **một Tech Spec** do team của BC đó sở hữu, đặt cạnh code, review qua PR (R-B16). Tech Spec chứa: module & component (L3), C&C, **deployment chi tiết per-BC** (→ IaC), domain/data nội bộ, key flows, **quyết định context-local** (R-I6). Khuôn mục cố định để các BC nhất quán (Context&Scope → Requirements → Design overview → Interfaces&data → Key flows → Operations → Decisions → Test → Open questions). AD **trỏ tới** Tech Spec qua bảng correspondence, không sao chép nội dung L3.

- **R-I4 — AD phân tầng trên một model liên-bang (federated).** Ở quy mô lớn: thay vì một AD/model nguyên khối, dùng **một AD landscape mỏng** (Landscape + Context Map + deployment-zone + contracts + decisions + index) **trên một model-as-code liên-bang**:
  - một **base workspace** định nghĩa mỗi BC là một `softwareSystem` (hộp) + quan hệ;
  - mỗi BC có **một workspace riêng `extends` base** rồi bơm container (L2) + component (L3) vào hộp của mình (`!element <bc> { … }`);
  - **correspondence tự động qua trùng identifier** (giảm bảng tay của R-B14): hộp BC ở base ⇄ container/component team bơm vào.
  - 42010 cho phép **nhiều AD + correspondence rules** — không cần một AD nguyên khối.

- **R-I5 — Grain của Deployment view khi scale (mở rộng R-A8/B8).** Deployment ở grain **L2 (container trên node)** — cùng grain Container view, là chỗ duy nhất mọi container hiện cùng lúc. Ở **≥10 BC** áp cùng luật tách:
  - **AD giữ grain BC/zone:** trust-zone, ánh xạ container→node *mức quyết định*, ràng buộc (egress, AZ/HA). Model-as-code dùng `softwareSystemInstance` (grain BC).
  - **Đẩy xuống Tech Spec/IaC:** số replica, ngưỡng HPA, sizing, và **deployment per-BC** (`containerInstance`). Đây là phần R-A8 vốn loại khỏi AD ("YAML/IaC, sizing cụ thể").

- **R-I6 — ADR phân cấp: register hệ-thống vs context-local.**
  - **ADR hệ thống** (`ADR-NNNN`, đánh số toàn cục) — quyết định nặng **liên-BC / cross-cutting**; sống ở AD register (R-A13) / `/docs/adr`.
  - **ADR context-local** (`ADR-<BC>-N`) — quyết định **nội bộ một BC**; sống trong Tech Spec của BC, **tham chiếu** tới ADR hệ thống liên quan. Không trộn hai cấp vào một dãy số.

- **R-I7 — Ngưỡng & lộ trình (gom R-B8 + R-I*).**

  | Quy mô | Structure view | Deployment | L3 | Model |
  | --- | --- | --- | --- | --- |
  | **≤ ~7 BC** | Landscape + Context Map + **1 archetype + vài Container ví dụ** + bảng | 1 sơ đồ hệ thống còn đọc được | Tech Spec (khuyến nghị) | Mermaid/docs-as-code chấp nhận được |
  | **≥ ~10 BC / đa P&L** | Landscape + Context Map ở AD; **Container/Component per-BC RỜI về Tech Spec**, sinh từ model | **bắt buộc** tách: AD grain BC/zone, chi tiết → Tech Spec/IaC | **bắt buộc** ở Tech Spec | **bắt buộc** model-as-code liên-bang (R-I4) |

  Điều kiện chuyển: chạm ~10 BC, hoặc một Container/Deployment diagram bắt đầu "tất-cả-trong-một" (R-B4/G3), hoặc nhiều team tranh chấp sửa cùng một file AD.

- **R-I8 — Enforce:** R-I2/R-I3 kiểm bằng *review gate* (PR Tech Spec); R-I4 kiểm bằng *fitness*: model liên-bang parse + validate per-workspace trong CI, **drift check** so model với code/route/topic (R-E3/E4). Per-BC view không được nhúng tay vào AD trung tâm (lint cấu trúc repo).

## PHẦN D — QUY TẮC QUYẾT ĐỊNH (ADR)

- **R-D1 — Mọi quyết định nặng-kiến-trúc → 1 ADR:** quan trọng/đắt/khó đảo/rủi ro/quy mô lớn (theo tiêu chí arc42 §9).
- **R-D2 — Format chuẩn:** Context → Decision → Status → Consequences (Nygard) hoặc MADR; mỗi ADR một file, đánh số, immutable.
- **R-D3 — Vòng đời trạng thái:** `Proposed → Accepted → Superseded by ADR-xxx`. Đánh dấu superseded để log trung thực; không xóa ADR cũ.
- **R-D4 — Đặt cạnh code & tham chiếu được:** lưu trong version control (vd /docs/adr) để CI tham chiếu.
- **R-D5 — ADR phải đủ cụ thể để biến thành kiểm tra:** nếu Decision + Consequences không thể chuyển thành rule phát hiện được thì ADR chưa đủ rõ.
- **R-D6 — Liên kết ADR ↔ view ↔ fitness function:** mỗi ADR nặng nên trỏ tới view bị ảnh hưởng và (nếu có) fitness function bảo chứng nó.

---

## PHẦN E — QUY TẮC KIỂM SOÁT & TỰ ĐỘNG HÓA (Governance-as-Code)

> Cốt lõi AaC: chuyển quyết định từ "văn bản tĩnh" sang "kiểm tra chạy được". Fitness function tự động hóa governance trong deployment pipeline; ThoughtWorks Tech Radar xếp kỹ thuật này ở mức "Adopt".

### E.1 Quy tắc

- **R-E1 — Quyết định có thể tự động → fitness function (P6):** mỗi quyết định kiểm-được phải có một kiểm tra tự động chạy trên mỗi build/deploy. Decision record ghi quyết định; fitness function bảo chứng nó.
- **R-E2 — Quyết định cần phán đoán → human gate:** luồng tiền mới, truy cập xuyên tenant, đổi policy bất biến, breaking change hợp đồng, mỗi cột mốc ZTA → review người bắt buộc.
- **R-E3 — Validate model trong CI:** workspace/DSL phải parse & validate (tính hợp lệ C4, phần tử mồ côi, vi phạm naming) trong pipeline.
- **R-E4 — Drift detection:** CI so model với hiện thực (dependency thực tế, route, topic Kafka) — phát hiện sơ đồ lệch code; lệch là cảnh báo, không im lặng.
- **R-E5 — Mọi quy tắc binding có cơ chế enforce:** mỗi dòng "binding" ở PHẦN A/B/C phải map tới một fitness function hoặc một gate; không có quy tắc "chỉ nằm trên giấy".

### E.2 Ví dụ ánh xạ Quyết định → Fitness function (mẫu)

| Quyết định (ADR) | Fitness function (máy kiểm) |
| --- | --- |
| DB-per-context, không FK xuyên context | Quét migration/schema: cấm FK trỏ DB khác |
| Event versioning backward-compatible | Schema-registry compatibility check trong CI |
| API versioning, cấm breaking ngầm | Contract test từ chối thay đổi phá vỡ thiếu version mới |
| Mọi consumer idempotent (at-least-once) | Inject event trùng → assert không xử lý 2 lần |
| Payment egress chỉ tới PG/Bank | Network policy check |
| Mọi service-to-service là mTLS | Mesh policy test |
| Data residency / vùng cho phép | Policy chặn deploy sai region |
| Per-BC view không nhúng tay vào AD trung tâm (R-I4) | Lint cấu trúc repo: AD chỉ chứa landscape/context-map; container/component sinh từ model |
| Model liên-bang nhất quán (R-I4/I8) | CI: mỗi workspace `extends` parse + validate; drift check model ↔ code/route/topic |
| L3 không rò vào AD (R-I2) | Lint: file AD không chứa diagram mức component cho ≥ ngưỡng BC |

---

## PHẦN F — BỐ CỤC REPO & PIPELINE (AaC cụ thể)

```text
/docs/architecture/
  ├── ad/                      # tài liệu AD (markdown, version-controlled)
  │   ├── SDD-<system>.md
  │   └── views/               # ảnh render (output, sinh tự động)
  ├── model/
  │   └── workspace.dsl        # SINGLE SOURCE OF TRUTH (Structurizr DSL)
  ├── adr/                     # 1 file / quyết định, đánh số
  │   ├── 0001-....md
  │   └── 0002-....md
  ├── contracts/
  │   ├── openapi/             # hợp đồng sync (validated CI)
  │   └── asyncapi/            # hợp đồng event + schema registry refs
  └── fitness/                 # fitness functions / policy-as-code
.ci/
  └── architecture.yml         # validate model · render views · drift check · contract/compat check
```

- **R-F1:** view trong AD nhúng từ `views/` (output), không vẽ tay lại.
- **R-F2:** pipeline `architecture.yml` chạy: parse+validate model → render → drift check → contract/compat check → fitness functions; fail = chặn merge.
- **R-F3:** AD/ADR/model/contract cùng review trong một PR khi thay đổi liên quan (giữ correspondence).

---

## PHẦN G — ANTI-PATTERNS (cấm)

| # | Anti-pattern | Vì sao sai |
| --- | --- | --- |
| G1 | Schema/field/mã lỗi gõ tay trong AD | Stale tức thì; vi phạm R-A15/R-C2 |
| G2 | Tên RPC method trên sơ đồ kiến trúc | Kéo AD xuống interface-spec; dễ sai (R-B10) |
| G3 | Một Container diagram "tất-cả-trong-một" cho 10+ context | Rối, mất câu chuyện; vi phạm R-B4/R-B8 |
| G4 | Database trôi nổi ngoài context | Lệch grain, mất traceability (R-B7) |
| G5 | Liệt kê framework từng service ở AD như quyết định | Couple AD vào lựa chọn lật được (R-A20) |
| G6 | ADR viết xong không ai thực thi | "Documentation theater" (R-E1) |
| G7 | Vẽ tay nhiều bản trùng thông tin | Lệch nhau khi đổi; vi phạm P1/R-B1 |
| G8 | "Tin theo vị trí mạng" (trong VPC = được tin) | Phá Zero-Trust; áp cho cả ranh giới liên-tổ-chức |
| G9 | Event coi là nội bộ publisher | Phá liên kết lỏng; vi phạm R-C3 |
| G10 | Trộn nhiều mức trừu tượng trong một view | Khó đọc; vi phạm R-B4 |

---

## PHẦN H — CHECKLIST DEFINITION-OF-DONE CHO AD

| # | Hạng mục | ✓ |
| --- | --- | --- |
| 1 | Đủ 17 khối nội dung bắt buộc (R-A1…A17) hoặc đánh dấu "N/A — lý do" | ☐ |
| 2 | Mọi view sinh từ một model nguồn; không vẽ tay trùng (R-B1) | ☐ |
| 3 | Zoom đa tầng + Context Map; datastore nằm trong context (R-B5…B9) | ☐ |
| 4 | Nhãn = ý định + protocol; không tên method; có legend (R-B10/B11) | ☐ |
| 5 | Quy ước tên BC/Service nhất quán xuyên tài liệu (R-B12) | ☐ |
| 6 | Bảng correspondence/traceability đầy đủ, không hộp "lửng" (R-B13/B14) | ☐ |
| 7 | Công nghệ phân loại binding vs indicative; nêu quyết định polyglot (R-A20) | ☐ |
| 8 | Hợp đồng sync→OpenAPI, async→AsyncAPI+registry; AD chỉ trỏ (R-C2/C3) | ☐ |
| 9 | Bảng đảm bảo tương tác đầy đủ (R-C4) | ☐ |
| 10 | Mọi quyết định nặng có ADR; trạng thái & rationale (R-D1…D6) | ☐ |
| 11 | Quyết định kiểm-được có fitness function; cần phán đoán có gate (R-E1/E2) | ☐ |
| 12 | Model validate + drift check + contract/compat check pass trong CI (R-E3…E5) | ☐ |
| 13 | TBD đánh dấu tường minh + ADR/issue theo dõi (R-A22) | ☐ |
| 14 | Version + changelog + ADR cập nhật (R-B15) | ☐ |
| 15 | AD/ADR/model/contract cùng repo, review qua PR (R-B16/F3) | ☐ |
| 16 | (Hệ lớn) Grain C4 rõ: BC=Landscape, service/DB=L2, component=L3 (R-I1) | ☐ |
| 17 | (Hệ lớn) AD dừng ở L2; L3 ở Tech Spec; 1 BC = 1 Tech Spec team-owned (R-I2/I3) | ☐ |
| 18 | (Hệ lớn ≥10 BC) Container/Component per-BC rời AD, sinh từ model liên-bang (R-I4/I7) | ☐ |
| 19 | (Hệ lớn) Deployment: AD grain BC/zone; sizing/per-BC → Tech Spec/IaC (R-I5) | ☐ |
| 20 | ADR phân cấp: register hệ-thống (ADR-NNNN) vs context-local (ADR-&lt;BC&gt;-N) (R-I6) | ☐ |

---

## PHỤ LỤC — Bảng ánh xạ chuẩn

| Khối nội dung (bộ quy tắc) | 42010:2022 | arc42 § | C4 / artifact |
| --- | --- | --- | --- |
| Stakeholders, concerns, goals | stakeholder, concern, aspect | 1 | — |
| Constraints | (constraint) | 2 | — |
| Context & scope | architecture view | 3 | C4 L1 (Context) |
| Solution strategy | architecture decision | 4 | — |
| Structure views | viewpoint, view, view component, model kind, legend | 5 | C4 Landscape/Container/Component (Structurizr) |
| Runtime views | view (behavioral) | 6 | Sequence/flow |
| Deployment | view (deployment) | 7 | C4 Deployment |
| Data / Security / Cross-cutting | aspect, stakeholder perspective | 8 | — |
| Quality | concern, aspect | 10 | Quality tree + scenarios |
| Decisions | architecture decision, architecture rationale | 9 | ADR (Nygard/MADR) |
| Risks & debt | concern | 11 | — |
| Glossary | — | 12 | — |
| Traceability | correspondence, correspondence method | (xuyên suốt) | Structurizr model relations |
| Enforcement | — | (governance) | Fitness functions / policy-as-code |

## Tài liệu nguồn

1. ISO/IEC/IEEE 42010:2022 — Software, systems and enterprise — Architecture description. (iso-architecture.org/ieee-1471/cm/ — conceptual model)
2. arc42 — https://docs.arc42.org/home/ ; https://arc42.org/overview
3. C4 model — https://c4model.com ; Structurizr "as code" — https://docs.structurizr.com/as-code
4. N. Ford, R. Parsons, P. Kua, P. Sadalage — *Building Evolutionary Architectures* (2nd ed., O'Reilly, 2023) — fitness functions & automating governance.
5. M. Nygard — Architecture Decision Records; MADR — https://adr.github.io
6. Diagrams as Code 2.0 (S. Brown) — tách authoring/rendering, một model nhiều view.
