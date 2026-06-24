# BASELINE — QUAN SÁT & GIÁM SÁT (Org Observability Baseline)

| Thông tin | Giá trị |
| --- | --- |
| Mã | `STD-OBS-v1.0` |
| Loại | **Baseline nội dung** — default tổ chức |
| Neo chuẩn | OpenTelemetry · Google SRE (Golden Signals) · RED method |
| Quan hệ | AD/Tech Spec **conform** + ghi delta (metric/alert/dashboard đặc thù nghiệp vụ) |

> **Cách dùng:** AD mục "Observability" ghi *"Conform `STD-OBS-v1.0`"* + **metric/alert/dashboard đặc thù** của hệ thống. Convention chung dưới đây không lặp ở AD.

---

## 1. Logging (default)

- **Structured JSON**; mask PII/secret/số tài khoản.
- **Audit log bất biến** (Object Lock) cho sự kiện nhạy cảm; retention theo policy.
- Trường tối thiểu: `timestamp, level, service, traceId, requestId`. Hệ thống multi-tenant **thêm `tenantId`/`merchantId`**.

## 2. Metrics (default)

- **RED** mỗi service (Rate, Errors, Duration) + **Golden Signals** (latency, traffic, errors, saturation).
- Business metrics (đặc thù) → AD/Tech Spec khai báo riêng, đặt tên `business_*`.

## 3. Distributed Tracing (default)

- **OpenTelemetry**; propagate trace context qua **mọi hop** (HTTP/gRPC/message bus).
- Sampling mặc định: **100% errors**, **5% normal** (điều chỉnh theo tải).

## 4. Alerting (default)

| Severity | Ý nghĩa | Kênh mặc định |
| --- | --- | --- |
| **P1** | Mất tiền/dữ liệu, ngừng dịch vụ lõi | PagerDuty (+ on-call) |
| **P2** | Suy giảm, có nguy cơ lan | Slack + runbook |
| **P3** | Cần theo dõi, chưa khẩn | Ticket |

Mỗi alert phải nối **runbook** + ngưỡng rõ ràng. Alert đặc thù nghiệp vụ → AD.

## 5. Dashboard taxonomy (default)

`Service Overview (SRE)` · `Business KPIs (PO)` · `SLA/SLO` · `Security` · (tùy hệ) `Finance`. Nội dung từng dashboard đặc thù → AD.

---

**Changelog:** v1.0 (2026-06-24) — tách từ AD-Marketplace-AiGen §9 thành baseline tổ chức.
