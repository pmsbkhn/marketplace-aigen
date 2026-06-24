# BASELINE — KHẢ NĂNG PHỤC HỒI & DR (Org Resilience/DR Baseline)

| Thông tin | Giá trị |
| --- | --- |
| Mã | `STD-RES-v1.0` |
| Loại | **Baseline nội dung** — default tổ chức |
| Neo chuẩn | Release It! (resilience patterns) · ISO 22301 (BCM) · arc42 §10 |
| Quan hệ | AD/Tech Spec **conform** + ghi delta; **target RTO/RPO sống ở NFR catalog** (R-E6), baseline chỉ định nghĩa *khung tier* + *pattern* |

> **Cách dùng:** AD mục "Xử lý lỗi & phục hồi" ghi *"Conform `STD-RES-v1.0`"* + **phân tier cụ thể** (trỏ NFR-DR-* ở catalog) + **invariant đặc thù**. Không restate RTO/RPO (đã ở catalog).

---

## 1. Error model chuẩn

- HTTP status: `400/401/403/404/409/422/429/500/503` theo ngữ nghĩa chuẩn.
- Body lỗi: `{ error: { code, message, details[] } }`.
- Hành động chi tiết per-mã → **runbook** (ngoài AD).

## 2. Resilience patterns (default)

| Pattern | Khi dùng |
| --- | --- |
| **Saga + compensation** | Giao dịch phân tán nhiều bước; bước sau lỗi → bù trừ **ngược thứ tự** |
| **Idempotency (bắt buộc)** | Mọi money-op, webhook, thao tác không-an-toàn khi lặp |
| **Circuit Breaker** | Gọi phụ thuộc ngoài (cổng/bank/3rd-party) |
| **Timeout mọi I/O** | Chống treo lan truyền; trần ≠ budget độ trễ |
| **Retry + DLQ** | Lỗi tạm thời; quá hạn → dead-letter |
| **Graceful degradation** | Phụ thuộc lỗi → fallback (cache/clamp/fail-safe), không trả kết quả sai |

## 3. Khung phân tier DR (org standard)

| Tier | Tên | RTO | RPO |
| --- | --- | --- | --- |
| **1** | Critical | < 1h | < 5min |
| **2** | Business | < 4h | < 1h |
| **3** | Important | < 24h | < 4h |

> AD **gán BC vào tier** và đặt **NFR-DR-ID** ở catalog (R-E6). Baseline chỉ định nghĩa **band**; con số target không lặp ở mục DR của AD.

## 4. Kế hoạch DR chuẩn (default)

1. Failover **multi-AZ**; service stateless rebuild.
2. **Restore từ backup** (tần suất theo tier).
3. Dữ liệu bất biến: **cross-region replication** (WORM nếu là chứng từ).
4. **Smoke-test luồng nghiệp vụ trọng yếu** trước khi công bố phục hồi.
5. **Post-mortem** trong 48h.

## 5. Invariant phục hồi chuẩn (enforce → AaC)

- Saga thất bại **không để lại trạng thái mồ côi** (reservation/pending orphan).
- Consumer **idempotent**; eventual consistency có **reconcile + monitoring**.
- Mọi phụ thuộc ngoài có **timeout + circuit breaker**.

---

**Changelog:** v1.0 (2026-06-24) — tách từ AD-Marketplace-AiGen §8 thành baseline tổ chức.
