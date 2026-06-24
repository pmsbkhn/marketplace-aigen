# BASELINE — AN TOÀN AI (Org AI-Security Baseline)

| Thông tin | Giá trị |
| --- | --- |
| Mã | `STD-AISEC-v1.0` |
| Loại | **Baseline nội dung** — default tổ chức |
| Neo chuẩn | OWASP Top 10 for LLM Applications · NIST AI RMF · MITRE ATLAS |
| Quan hệ | **Kích hoạt khi** hệ thống có thành phần AI/LLM. Hệ không dùng AI → AD ghi *"N/A — lý do"* + trỏ baseline này |

> **Cách dùng:** AD mục "AI Security" nếu **không** có AI → một dòng *"N/A — không có thành phần AI/LLM; nếu thêm, conform `STD-AISEC-v1.0`"*. Nếu **có** AI → AD address từng hạng mục checklist dưới đây + delta.

---

## Checklist khi đưa AI/LLM vào hệ thống

| # | Hạng mục | Yêu cầu tối thiểu |
| --- | --- | --- |
| 1 | **Prompt injection** | Tách dữ liệu không tin khỏi instruction; output không được tự ý thực thi hành động đặc quyền |
| 2 | **Output handling** | Validate/escape output trước khi dùng (chống XSS/SSRF/command injection gián tiếp) |
| 3 | **Data leakage** | Không để model lộ PII/secret/dữ liệu xuyên tenant; kiểm soát context đưa vào prompt |
| 4 | **Tool/agent authz** | Mọi tool/function-call qua **PDP/PEP** như service thường (PoLP, deny-by-default) |
| 5 | **Supply chain model** | Pin version model/weights; provenance; quét dependency |
| 6 | **Rate/cost abuse** | Quota + rate limit theo tenant; chống prompt-bомbing |
| 7 | **Audit & trace** | Log prompt/response (mask PII) bất biến; trace qua OTel như mọi hop |
| 8 | **Human-in-the-loop** | Hành động không-thể-đảo (tiền, xóa, gửi ra ngoài) cần xác nhận người |

> Mỗi hạng mục có rủi ro cao → **gate review** + có thể cần **ADR riêng**.

---

**Changelog:** v1.0 (2026-06-24) — tạo mới; AD-Marketplace-AiGen §10 hiện N/A, trỏ về baseline này.
