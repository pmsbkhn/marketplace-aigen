# 1. Tách runtime observability khỏi governance (fitness) plane

- Status: Accepted
- Date: 2026-06-13
- Context bị ảnh hưởng: toàn hệ thống (observability, EA fitness)
- Nguồn: back-fill từ commit #22 ("SLO / runtime-fitness Grafana dashboard") khi hợp nhất
  register ADR vào repo. Quyết định gốc có trước file này.

## Context

Hệ thống vừa cần **giám sát vận hành** (SLI/SLO thời gian thực cho ops), vừa cần **chấm
điểm tuân thủ kiến trúc** (EA fitness verdict cho governance). Hai nhu cầu này khác bản
chất: một bên là chuỗi số đo liên tục cho người trực, một bên là phán quyết pass/fail/waive
cho hội đồng kiến trúc. Trộn chúng vào một nơi gây nhiễu cả hai.

## Decision

Tách làm **hai mặt phẳng (plane)**:

- **Runtime plane (ops)**: các SLI nằm trong **Prometheus + Grafana** ở prod. Dashboard
  "SLO / runtime fitness" hiển thị `runtimeObjectives` của registry kèm ngưỡng: HTTP
  latency tối đa mỗi service (SLO < 1s, warn 0.5), outbox pending (< 500), outbox parked
  (= 0), và panel latency drift (now vs 1h trước).
- **Governance plane (EA)**: **verdict** suy ra từ các SLI đó (pass/warn/fail/waived) đi
  về EA scorecard — chính là dòng `FitnessResult`/`EA_FITNESS_RESULT` mà CI thu thập
  (xem `.github/workflows/fitness.yml`).

## Consequences

- (+) Ops xem số liệu thời gian thực; governance xem phán quyết — không lẫn lộn.
- (+) Cùng một `runtimeObjectives` registry là nguồn cho cả ngưỡng dashboard lẫn verdict.
- (−) Phải duy trì ánh xạ SLI → verdict nhất quán giữa hai plane.

## Liên quan

- `deploy/observability/` (Prometheus/Grafana/Tempo). [ADR-0002](0002-msfw-stringidentity.md)
  và các fitness function thuộc cùng "governance plane".
