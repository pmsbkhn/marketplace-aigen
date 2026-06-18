# Triển khai (Deployment) & Bảo mật

## Topology (môi trường local k3d)

![Production Deployment](embed:ProdDeployment)

- **marketplace ns**: 6 pod (Istio sidecar), mỗi pod có OpenTelemetry javaagent xuất
  trace sang **Tempo**; `/actuator/prometheus` được Prometheus scrape.
- **infra ns**: PostgreSQL, Kafka (Strimzi/KRaft), **Redis & Elasticsearch** (đã
  provision nhưng app **chưa wire** — Phase C), observability (Prometheus/Grafana/Tempo).
- **Istio** cung cấp ingress gateway + mesh; **mTLS STRICT + authz** là Phase D.

> Khác biệt quan trọng: **container view** mô tả app wiring hiện tại (REST/in-memory/
> DB-search), còn **deployment view** mô tả hạ tầng đã có. Redis/ES nằm ở deployment
> nhưng chưa xuất hiện trong container view vì code chưa dùng.

## Bảo mật

![Security Boundaries](embed:SecurityBoundaries)

Tô màu viền theo tag: `Ingress` (công khai), `InternalApi` (S2S /internal/*),
`Sensitive` (tiền/PII/secret), `External`, `Standin`. Webhook gateway đã xác minh
HMAC + chống replay.
