# QUY TẮC NỘI DUNG & CÁCH VIẾT TÀI LIỆU KIẾN TRÚC
## (AD/SDD + Tech Spec — viết tay; áp dụng từ 1 BC, scale lên N BC)

| | |
| --- | --- |
| Mã | `STD-DESIGN-DOC-v1.2` |
| Trạng thái | Draft for review |
| Phạm vi | AD/SAD/SDD (cấp hệ thống) + Tech Spec (cấp miền), Markdown · **áp dụng kể cả khi hệ chỉ có 1 BC** |
| Neo chuẩn | ISO/IEC/IEEE 42010:2022 · arc42 · C4 · ADR (Nygard/MADR) · DDD · ASR/ATAM (SEI) |
| Thay đổi v1.2 | **Chẻ đôi phạm vi thành Lớp A (Prose — viết & giữ tay) và Lớp B (Model Obligations — khai lúc tạo, duy trì khi có công cụ)** (§0.3), gỡ mâu thuẫn "metamodel khoác áo viết tay". Chuẩn hóa cho **1 BC** + khung liên-BC để chỗ sẵn (§2). Phân tầng Core/Extension **theo nguyên tắc + trigger đếm-được** (§3). Thêm **bộ lọc ASR** cho yêu cầu (§5). **Luật `verify:`**: khi nào `review` không đủ + giá trị `audit`; nêu rõ quan hệ field `verify:` (Lớp A) ↔ cạnh `verifies` (Lớp B) (§8.1). Kịch bản thay đổi thêm **likelihood × impact** (§8.3). Thêm **brownfield as-is↔to-be** (§8.4). **Luật ID** ổn định (§6). **Cadence tái-soát** in-scope (§10). DDD theo **trục** thay vì enum (§7.1). |

---

## 0. Mục đích, phạm vi, hai lớp & giới hạn

### 0.1 Mục đích
Viết được AD/SDD + Tech Spec **đúng & đủ bằng Markdown thuần**: cần mục nào, mỗi mục chứa gì, viết thế nào — **kể cả hệ chỉ có một bounded context**, sao cho mở rộng sau này là *thêm vào*, không *đập đi xây lại*.

### 0.2 Vết cắt phạm vi
- **TRONG phạm vi:** *thông tin* + *quy trình do người làm* (khuôn mục, luật viết, "N/A — lý do", vòng đời ADR, **cadence tái-soát**, checklist, review gate).
- **NGOÀI phạm vi:** *mã hóa/biểu diễn bằng công cụ* và *cưỡng chế tự động* (sinh view từ một model, kiểm nhất quán/drift bằng máy, validate pipeline).

> Ranh giới thật = **quyết-định-&-quy-trình-người** (trong) vs **mã-hóa-&-cưỡng-chế-máy** (ngoài).

### 0.3 Hai lớp TRONG phạm vi (điểm cốt lõi v1.2)
Không phải mọi nghĩa vụ in-scope đều giữ-bằng-tay-được như nhau. Chia rõ:

| Lớp | Gồm | Tạo (t0) | Duy trì theo thời gian |
| --- | --- | --- | --- |
| **A — Prose Standard** | khuôn mục · luật viết W* · bộ lọc L*/ASR · ADR-file + index · glossary · kịch bản thay đổi · cadence tái-soát · quyết định | **viết tay** | **giữ bằng tay** (đây là lời hứa §0.1) |
| **B — Model Obligations** | đồ thị truy vết có ID + ngữ nghĩa cạnh (§6) · phủ-concern (`frames`) · manifest lát-cắt (§10) | **khai bằng tay** (rẻ ở t0) | **`declared-now, maintained-when-tooled`** — *không* là nghĩa vụ tay thường trực |

**Vì sao tách:** giữ một *đồ thị nhiều-nhiều có kiểu cạnh nhất quán xuyên nhiều file* bằng tay là việc §0.4 nói không co giãn theo sức người. Lớp B vì thế được **khai một lần lúc viết** (như khế ước với lớp công cụ sau), **không** bắt người cập nhật tay mỗi thay đổi. Lớp A mới là thứ con người vừa viết vừa giữ lâu dài.

### 0.4 Giới hạn — chuẩn *bootstrapping*
Viết tay cho ra **v1.0 đúng**. Nhưng ở quy mô (nhiều BC/đội), các bảo đảm sau **không** co giãn theo người và sẽ rữa nếu chỉ review tay → là **Lớp B**, duy trì khi có công cụ:

| Bảo đảm | Tạo tay | Duy trì ở quy mô |
| --- | --- | --- |
| Nhất quán đa-view (§7) | được | cần công cụ |
| Truy vết đầy đủ, không cạnh gãy (§6) | được | cần công cụ |
| Không drift (tài liệu ↔ hiện thực) | đúng tại t0 | cần công cụ |

Có một **cửa sổ** giữa "hệ đủ lớn để giữ-graph-bằng-tay là gánh nặng âm" và "công cụ chưa tới" — Lớp B nhận đúng cửa sổ đó bằng nhãn *maintained-when-tooled*, thay vì giả vờ là việc tay thường trực.

---

## 1. Nguyên lý nội dung

| # | Nguyên lý | Hệ quả |
| --- | --- | --- |
| N1 | Đúng mức trừu tượng cho đúng người đọc | mỗi mục/sơ đồ một grain |
| N2 | Lõi ổn định, đẩy chi tiết xuống | AD giữ thứ ít đổi; chi tiết → Tech Spec/contract/code |
| N3 | Chỉ ghi cái bên ngoài phụ thuộc | bề mặt/hợp đồng vào tài liệu; nội bộ tự do đổi → tầng dưới |
| N4 | Truy vết được | nối bằng ID + ngữ nghĩa cạnh (Lớp B, §6) |
| N5 | Chưa chốt thì nói thẳng (TBD) | `TBD` + nơi theo dõi |

> Chuẩn áp N2/N3 cho chính nó: cấu trúc **không phẳng** — có **Core (mọi AD, kể cả 1 BC)** + **Extension bật theo trigger đếm-được** (§3); và tách **Lớp A giữ-tay** khỏi **Lớp B khai-một-lần** (§0.3).

---

## 2. Hai loại tài liệu & nguyên tắc 1-BC-đến-N-BC

| Loại | Trả lời | Phạm vi | Sở hữu |
| --- | --- | --- | --- |
| **AD / SDD** | *Vì sao chia & liên kết thế này?* | toàn hệ | Kiến trúc sư / Platform |
| **Tech Spec** | *Context này chạy ra sao?* | **một** BC | Team của BC |

**1 BC = 1 Tech Spec.** AD trỏ tới Tech Spec, không sao chép L3.

**Nguyên tắc mở rộng (bắt buộc):** một hệ **1 BC là instance hợp lệ** của chuẩn này. Các mục **liên-BC** (Context Map §7.1, correspondence physical §7.2, index BC) **luôn có chỗ dành sẵn** trong khuôn; ở 1 BC chúng ghi *"1 BC — kích hoạt khi có BC thứ 2"*. Lên N BC = **điền vào chỗ đã có**, không tái cấu trúc tài liệu. (Đây là lý do single-BC vẫn phải theo chuẩn.)

---

## 3. Cấu trúc AD/SDD

> Cột **Lớp**: A = prose giữ-tay · B = model obligation (khai-một-lần). Cột **Tầng**: `Core` = mọi AD kể cả 1 BC · `Ext(trigger)` = bật theo điều kiện **đếm-được/tier/miền** (không dùng trigger kiểu "có dữ liệu" vì gần như luôn đúng). Header luôn có: mã · semver · trạng thái · ngày · tác giả/duyệt · **lịch sử thay đổi** · **last-validated** (§10) · mức bảo mật.

| Mục AD | Lớp | Tầng | Phải chứa | Đẩy xuống |
| --- | --- | --- | --- | --- |
| **1. Tổng quan** (mục tiêu+KPI · phạm vi in/out · stakeholders & **concern** · ràng buộc) | A | Core | KPI đo được; mỗi stakeholder ≥1 concern (concern phải được view `frames` — §6) | KPI vận hành; config |
| **2. Kiểu kiến trúc + nguyên tắc** | A | Core | style + lý do; nguyên tắc chủ đạo | — |
| **3. View cấu trúc** (Context L1 · BC=hộp · Container/BC L2 · Component L3 *khi cần*) | A | Core | ghi chú grain (W5); mỗi view khai **`frames:`**; binding vs indicative (+vòng đời §5) | component nội bộ; runtime per-service |
| **4. Luồng & hành vi** (happy/compensation/async) grain hệ thống | A | Core | luồng chính + lỗi/saga | pseudocode; bước nội bộ service |
| **5. Hợp đồng giao tiếp** (interface/event + **bảng bảo đảm tương tác**) | A | Core | sync/async, consistency, idempotency, ordering, delivery, lỗi; **trỏ** OpenAPI/AsyncAPI | field/method/mã lỗi |
| **6. Dữ liệu** (sở hữu theo context · reference logic · phân loại+retention · bất biến) | A | **Core** (chiều sâu = Ext) | quyền sở hữu + bất biến dữ liệu (baseline); ERD/lifecycle chi tiết = Ext | schema cột; DDL |
| **7. Bảo mật** (trust boundary · authn/authz · mã hóa · **threat model ref** · data residency · ranh giới secrets · **SBOM/supply-chain** · invariant + `verify:`) | A | **Core** (chiều sâu compliance = Ext) | baseline luôn có; chiều sâu bật khi tiền/PII/compliance | IAM literal |
| **8. Chất lượng & tiến hóa** (NFR + `verify:` · kịch bản vận hành · **kịch bản thay đổi**) | A | **Core** (NFR + change scenarios); cây chất lượng = Ext(≥2 thuộc tính cạnh tranh) | §8 | — |
| **9. Quyết định (ADR Index)** | A | Core | index ADR nặng (id·tiêu đề·trạng thái); ADR = file riêng (§9) | nguyên văn ADR |
| **10. Rủi ro & Nợ kỹ thuật** | A | Core | rủi ro/nợ + tác động + theo dõi | — |
| **11. Quan sát (Observability)** | A | **Core** baseline (chỉ số chính); chiều sâu = Ext(tier) | logging/metrics/tracing/alert nguyên tắc | cấu hình agent |
| **12. DR / phục hồi** | A | Ext(**tier ≥ business-critical**) | resilience + RTO/RPO theo tier | runbook |
| **Context Map** (correspondence logical, §7.1) | A | **Ext(≥2 BC)** — *chỗ dành sẵn ở 1 BC* | quan hệ BC↔BC theo trục (§7.1) | — |
| **Correspondence physical** (§7.2) | A | **Ext(≥2 BC hoặc topology không tầm thường)** — *chỗ dành sẵn* | BC ⟷ N container ⟷ deployment (nhiều-nhiều) | sizing → IaC/Tech Spec |
| **Truy vết (đồ thị)** | **B** | Core (khai) | ID + cạnh (§6); duy trì khi có công cụ | — |
| **13. Mục theo miền** | A | Ext(**miền**: AI/fintech/y tế…) | nội dung theo miền hoặc "N/A — lý do" | — |
| **Glossary** | A | Core | thuật ngữ + **Bounded Context** + bí danh tránh | — |
| **Phụ lục** | A | Core | tham chiếu + bảng SAD↔Tech Spec + checklist + phê duyệt + **manifest lát-cắt** (§10) | — |

### Luật viết (W)
- **W1 — NFR đo được** (số+đơn vị · cách đo · nguồn · phạm vi · `verify:`). Đo-được là *cần*, chưa *đủ* — phải qua **bộ lọc ASR** (§5).
- **W2 — View khai `frames:`** concern (đóng adequacy 42010 — §6).
- **W3 — Một sơ đồ một grain.**
- **W4 — Hợp đồng nêu *đảm bảo*, trỏ đặc tả** (field đầy đủ ở OpenAPI/AsyncAPI).
- **W5 — Ghi chú grain C4:** BC = hộp Landscape (≈L1) · service+datastore = L2 · component nội bộ = L3. "Bên trong BC" ≠ "L3".

---

## 4. Cấu trúc Tech Spec (per BC — kể cả khi là BC duy nhất)

**Header:** Status · Owner · Reviewers · **Liên kết lên AD** + OpenAPI/AsyncAPI + IaC · Classification (tier + data class) · **Khối "Ranh giới tầng"** (AD giữ C4 L2 gì · Tech Spec giữ C4 L3 gì · đẩy xuống nữa gì).

| Mục | Phải chứa |
| --- | --- |
| 1. Context & Scope | ranh giới BC, trust boundary, goals & non-goals |
| 2. Requirements | FR + NFR/SLO (mỗi cái `verify:`), nối về NFR hệ thống |
| 3. Design overview | 3.1 Module · 3.2 C&C + connector · 3.3 Deployment per-BC (→ IaC) |
| 4. Interfaces & data | API (ngữ nghĩa, trỏ OpenAPI) · domain model + invariant + `verify:` · data/schema · config · dữ liệu cá nhân |
| 5. Key flows | happy / compensation / fail-fast |
| 6. Operations & Resilience (delta) | phần khác/cụ thể hóa so với platform |
| 7. Decisions (context-local `ADR-<BC>-N`) + cross-cutting | tham chiếu ADR hệ thống |
| 8. Test strategy | unit/contract/integration/failure-injection + acceptance |
| 9. Open questions | TBD nội bộ BC |

---

## 5. Bộ lọc nội dung (3 chiều: chi tiết · công nghệ · YÊU CẦU)

**5.1 Chi tiết hiện thực (đặt ở đâu):** L1 Tier Test · L2 Dependency Test · L4 field-mang-sức-nặng · L5 TBD tường minh. AD dừng ở L2; L3 → Tech Spec.

**5.2 Công nghệ (binding vs indicative + vòng đời):** vào AD chỉ khi **load-bearing**; runtime/framework per-service là indicative → Tech Spec. **Vòng đời:** indicative tích đủ kẻ phụ thuộc (≥k context) → **trigger tái-phân-loại** → viết ADR nâng cấp (chống ossify im lặng).

**5.3 Yêu cầu — bộ lọc ASR (mới, đối xứng với 5.1/5.2):** một yêu cầu vào AD **chỉ khi *nặng-kiến-trúc* (Architecturally Significant)** — thỏa ≥1:
- (a) **định hình cấu trúc** (ép một component/ranh giới/kiểu tích hợp);
- (b) **khó/đắt đảo** về sau;
- (c) **cross-cutting** (chạm nhiều BC/component);
- (d) **giá-trị-rủi-ro cao** cho một stakeholder (tiền/pháp lý/an toàn);
- (e) **buộc một đánh đổi** giữa các concern.

> Đo-được (W1) là *cần*; ASR là *đủ*. NFR đo-được nhưng **không định hình cấu trúc nào** ⇒ không vào AD (về requirements doc/Tech Spec). Đây là lỗ hổng đối xứng v1.2 vá: trước đây có lọc cho công nghệ & chi tiết, thiếu lọc cho yêu cầu.

---

## 6. Truy vết = ĐỒ THỊ (Lớp B — khai lúc tạo, duy trì khi có công cụ)

Đồ thị **nhiều-nhiều**; phần tử có **ID**, liên kết mang **ngữ nghĩa cạnh**:

| Cạnh | Ý nghĩa |
| --- | --- |
| `satisfies` | NFR/quyết định đáp concern/mục tiêu |
| `refines` | mục dưới làm mịn mục trên (Tech Spec ← AD; NFR ← goal) |
| `constrains` | ràng buộc/ADR giới hạn mục khác |
| `verifies` | phép kiểm chứng minh một mệnh đề (quan hệ với `verify:` — §8.1) |
| `supersedes` | ADR thay thế ADR cũ |
| `trades-off` | ADR đánh đổi giữa concern đối kháng (§9) |

**Luật ID (bắt buộc — cả §6 và §10 phụ thuộc):**
- **Namespace + unique:** `<HỆ>-<LOẠI>-NN`, vd `MKT-NFR-01`, `MKT-ADR-0007`, `MKT-CONCERN-03`. Loại: GOAL/CONCERN/NFR/ADR/VIEW/BC/REL/QS/CHG/TEST.
- **Bất biến:** ID **không đổi/không đánh số lại** sau khi phát hành. Loại bỏ bằng trạng thái `deprecated`/`superseded`, **không** xóa. (Đổi ID = gãy cạnh trong im lặng — đúng bệnh "dangling" §10 lo, nhưng trong-tài-liệu.)

**Adequacy (42010):** mỗi **concern** (mục 1) phải được **≥1 view `frames`** (W2). Concern không view nào frame = lỗ hổng (DoD).

> **Lớp B:** ở v1.0 chỉ cần gán ID + khai vài cạnh chủ chốt (rẻ). **Không** bắt giữ toàn đồ thị nhất quán bằng tay; duy trì đầy đủ là việc của lớp công cụ (§0.3).

---

## 7. Correspondence — Logical & Physical (chỗ dành sẵn ở 1 BC)

Ranh giới **miền** (BC) ≠ ranh giới **triển khai** (container) — *thường* trùng, **không luôn**. **Cấm giả định BC ≅ deployable.** Hai bảng (ở 1 BC: ghi "1 BC — kích hoạt khi có BC thứ 2"):

**7.1 Logical — Context Map (theo TRỤC, không enum sạch).** Một cạnh BC↔BC gắn nhãn theo **hai trục độc lập** (đừng ép một nhãn đơn):
- **Trục quyền-lực/tổ-chức** (ai conform, ai hấp thụ thay đổi): *Customer/Supplier · Conformist · Partnership · Shared Kernel*.
- **Trục cơ-chế-dịch** (chống/ phơi model): *Anti-Corruption Layer · Open Host Service · Published Language*.
- Một cạnh có thể mang một nhãn mỗi trục (vd "Customer/Supplier + ACL"). Đây là **hợp đồng chiến lược** — topo phụ thuộc mà N3 nói tới (khác hợp đồng *chiến thuật* sync/async ở mục 5).

**7.2 Physical (nhiều-nhiều).** BC ⟷ **N container** ⟷ deployment node/zone. Cho phép một BC trải nhiều deployable (API+worker+projection+nhiều store) và một deployable chứa nhiều BC (modular monolith). Mỗi hộp deployment = node hạ tầng HOẶC instance của container đã định nghĩa — không hộp "lửng".

---

## 8. Mệnh đề kiểm-chứng-được & Kịch bản

### 8.1 `verify:` (Lớp A — nguồn sự thật prose-native) + quan hệ với cạnh `verifies`
Mọi mệnh đề kiểm-chứng-được (NFR/SLO/kịch-bản-chất-lượng/invariant/bảo-đảm-tương-tác) mang **`verify:`** ∈ `review` · `test` · `monitor` · `check` · **`audit`** (tái-soát định kỳ).

- **`verify:` là dạng prose-native (Lớp A)** — nguồn sự thật khi viết tay. **Cạnh `verifies` (§6) là phóng chiếu Lớp B** của cùng quan hệ (test-node → mệnh đề), dựng/đối soát khi có công cụ. Không nhân đôi sự thật: viết `verify:`, cạnh `verifies` là biểu diễn graph của nó.
- **Luật chống `verify: review` rỗng:** mệnh đề mà vi phạm gây rủi ro **tiền / an toàn / bảo mật / toàn-vẹn-dữ-liệu / xuyên-tenant** **KHÔNG được** chỉ `review` — phải `test`/`monitor`/`check`. `review` chỉ chấp nhận cho mệnh đề *thiết-kế/cấu-trúc* không có chế độ lỗi runtime. (Nếu không, A9 thành ô tick rỗng.)

### 8.2 Kịch bản vận hành
Cây chất lượng + kịch bản *stimulus → response đo được*, nối về mục tiêu/NFR; mỗi kịch bản có `verify:`.

### 8.3 Kịch bản thay đổi/tiến hóa (Core — kể cả 1 BC) + likelihood × impact
"Ổn định" chỉ có nghĩa *tương đối với một phân phối thay đổi kỳ vọng*. AD **phải khai kịch bản thay đổi dự đoán** — thứ duy nhất cho phép phán xét ranh giới (kể cả "khi nào tách BC duy nhất này").

| Trường | Ví dụ |
| --- | --- |
| Thay đổi dự kiến | "thay search engine" · "thêm loại tenant X" · "tách BC này làm đôi" |
| **Khả năng × Tác động** | **(bắt buộc)** cao/trung/thấp × số BC-hợp-đồng-ADR bị chạm — để **xếp ưu tiên** |
| Ranh giới hấp thụ | mục/ADR/ACL được thiết kế để cô lập thay đổi này |

> **Chống gold-plating:** không thiết kế để hấp thụ *mọi* thay đổi. Chỉ dựng linh hoạt (ACL, điểm mở rộng) cho kịch bản **khả-năng × tác-động cao** (tinh thần ATAM). Kịch bản khả năng thấp → ghi nhận, **không** đầu tư cấu trúc.

### 8.4 Brownfield — as-is ↔ to-be (hệ đang di trú)
Kiến trúc thật thường viết *giữa đường*. Với hệ không-greenfield, AD phải ghép cặp **view as-is (hiện trạng) ↔ view to-be (đích)** + **trạng thái quá độ** (vd strangler: phần nào đã cắt sang mới, phần nào còn legacy). View/quyết định theo lộ trình ghi rõ **target vs current** (khôi phục từ v1.0). §8.3 lo *thay đổi tương lai*; §8.4 lo *quá độ hiện tại*.

---

## 9. ADR — format, phân cấp, lưu trữ, tradeoff

- **Format:** Context → Decision → Status → Consequences; `Proposed → Accepted → Superseded by …`; **immutable**.
- **File riêng, AD chỉ index.** ADR là log bất biến tăng vô hạn → nhét nguyên văn vào AD phá N2. Mỗi ADR một file (`MKT-ADR-NNNN-….md` / `ADR-<BC>-N-….md`); AD/Tech Spec **liệt kê index**.
- **Phân cấp:** ADR hệ thống `MKT-ADR-NNNN` (liên-BC, ở AD) vs context-local `ADR-<BC>-N` (nội bộ BC, ở Tech Spec, tham chiếu lên).
- **Sổ tradeoff:** ADR đánh đổi phải liên kết tới **concern đối kháng** nó hi sinh (cạnh `trades-off` — §6), để lý lẽ tradeoff không mất.

---

## 10. Versioning, lát cắt nhất quán & cadence tái-soát

- **Per-doc semver + changelog** (bump khi đổi nặng-kiến-trúc).
- **Lát cắt nhất quán (Lớp B):** hệ = 1 AD + N Tech Spec + nhiều ADR + OpenAPI/AsyncAPI version độc lập → tham chiếu chéo dễ **dangling**. **Manifest / valid-as-of** ghi bộ phiên bản khớp nhau.
- **Cadence tái-soát (Lớp A — in-scope, KHÁC drift-detection của máy):** mỗi tài liệu mang **`last-validated`** (ngày người cuối đối chiếu nội dung với thực tế) + **trigger rà bắt buộc:** thêm/tách BC · một ADR bị `superseded` · đổi hợp đồng breaking · sau mỗi cột mốc lộ trình. Manifest cho biết phiên bản *khớp nhau*; `last-validated` cho biết nội dung *gần đây có được đối chiếu chưa*. (`verify: audit` là cơ chế cho mệnh đề cần tái-soát định kỳ.)

---

## 11. Glossary
Bảng ở AD: ID · Thuật ngữ · Định nghĩa thống nhất · **Bounded Context** · bí danh tránh · ví dụ. Cùng một từ nghĩa khác ở hai BC = tín hiệu ranh giới.

---

## 12. Anti-patterns (về nội dung)

| # | Anti-pattern | Vì sao sai |
| --- | --- | --- |
| A1 | Gõ tay schema/field/mã lỗi đầy đủ trong AD | stale; thuộc OpenAPI/AsyncAPI (W4) |
| A2 | Runtime/framework per-service ở AD như quyết định | indicative, dễ lật (5.2) |
| A3 | Lặp cùng thông tin nhiều nơi | lệch khi đổi (N3) |
| A4 | Nhồi L3 của BC vào AD | sai tầng; AD phình + đội dẫm đè |
| A5 | Trộn nhiều grain một sơ đồ | N1/W3 |
| A6 | Khung phẳng, không phân Core/Ext & Lớp A/B | vi phạm N2/N3 cấp meta; nặng → rữa |
| A7 | Truy vết vẽ như chuỗi 1:1 | mất nhiều-nhiều (§6) |
| A8 | Concern không view nào `frames` | hổng adequacy 42010 |
| A9 | Mệnh đề trọng yếu chỉ `verify: review` | sân khấu — vi phạm luật §8.1 |
| A10 | Kịch bản thay đổi không khả-năng×tác-động | mời gọi gold-plating (§8.3) |
| A11 | Nhét nguyên văn ADR vào AD | phá N2 (§9) |
| A12 | Correspondence ép BC ≅ deployable | không biểu diễn BC-đa-deployable / monolith-đa-BC (§7) |
| A13 | **Coi Lớp B (đồ thị/manifest) là nghĩa vụ giữ-tay thường trực** | mâu thuẫn §0.3/0.4; gánh nặng âm trong "cửa sổ" |
| A14 | **NFR đo-được nhưng không qua ASR** vẫn nhét vào AD | làm phình AD bằng requirement không-nặng-kiến-trúc (5.3) |
| A15 | **Đổi/đánh số lại ID** đã phát hành | gãy cạnh im lặng (luật ID §6) |
| A16 | **Bỏ as-is↔to-be** cho hệ đang di trú | mù brownfield (§8.4) |
| A17 | Ép cạnh Context Map một nhãn DDD đơn | tranh luận taxonomy vô ích (§7.1) |
| A18 | Bỏ trống mục không "N/A — lý do" | mất dấu vết quyết định bỏ qua |

---

## 13. Checklist Definition-of-Done

**Lớp A (viết & GIỮ bằng tay — bắt buộc lâu dài):**

| # | Hạng mục | ✓ |
| --- | --- | --- |
| 1 | Đủ **Core** (mọi AD kể cả 1 BC); Ext đúng trigger đếm-được hoặc "N/A — lý do" | ☐ |
| 2 | Mục liên-BC (Context Map, physical) có **chỗ dành sẵn** (≥2 BC mới điền) (§2) | ☐ |
| 3 | NFR đo được **và** qua **bộ lọc ASR** (§5.3) | ☐ |
| 4 | Mệnh đề kiểm-chứng-được có `verify:`; mệnh đề trọng yếu **không** chỉ `review` (§8.1) | ☐ |
| 5 | Kịch bản thay đổi có **khả-năng × tác-động** (§8.3); as-is↔to-be nếu brownfield (§8.4) | ☐ |
| 6 | Binding/indicative + trigger tái-phân-loại (5.2); grain note (W5) | ☐ |
| 7 | Hợp đồng trỏ OpenAPI/AsyncAPI + bảng bảo đảm tương tác (mục 5) | ☐ |
| 8 | Mỗi BC 1 Tech Spec đúng khuôn + khối "Ranh giới tầng"; AD không sao chép L3 | ☐ |
| 9 | ADR file riêng + index; phân cấp; tradeoff link concern đối kháng (§9) | ☐ |
| 10 | Rủi ro & Nợ; Glossary có cột BC; `last-validated` + trigger rà (§10) | ☐ |

**Lớp B (khai lúc tạo; duy trì đầy đủ khi có công cụ — `declared-now`):**

| # | Hạng mục | ✓ |
| --- | --- | --- |
| 11 | Phần tử có **ID** đúng namespace + bất biến (§6) | ☐ |
| 12 | Vài cạnh chủ chốt khai (satisfies/refines/verifies/trades-off); **concern coverage** (mọi concern ≥1 view `frames`) | ☐ |
| 13 | **Manifest lát-cắt nhất quán** (§10) | ☐ |

---

## Phụ lục — Ánh xạ chuẩn

| Mục | 42010:2022 | arc42 § | C4 |
| --- | --- | --- | --- |
| Stakeholders/concerns/goals | stakeholder, concern, aspect | 1 | — |
| Constraints | constraint | 2 | — |
| Context & scope | architecture view | 3 | L1 |
| Solution strategy | architecture decision | 4 | — |
| View cấu trúc (+ `frames`) | viewpoint, view, model kind, legend, correspondence | 5 | Landscape/Container/Component |
| Luồng / hành vi | view (behavioral) | 6 | Sequence |
| Triển khai | view (deployment) | 7 | Deployment |
| Dữ liệu/Bảo mật | aspect, perspective | 8 | — |
| Chất lượng + kịch bản (vận hành & thay đổi) | concern, aspect | 10 | Quality tree + scenarios |
| Quyết định (ADR) | architecture decision, rationale | 9 | — |
| Rủi ro & nợ | concern | 11 | — |
| Glossary | — | 12 | — |
| Truy vết + adequacy (Lớp B) | correspondence, correspondence method | (xuyên suốt) | — |

> **Lớp A** là lời hứa "viết tay" của §0.1 — viết *và* giữ bằng tay. **Lớp B** là khế ước với lớp *mã-hóa + cưỡng chế tự động* (ngoài phạm vi): khai bằng tay ở t0, để công cụ duy trì khi tới. Hai lớp **không** cùng một hạng nghĩa-vụ — đó là điều khiến "viết tay trước, công cụ sau" trở nên thành thật.
