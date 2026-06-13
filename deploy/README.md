# Local k8s platform — marketplace e2e (Phase A)

Reproducible local Kubernetes environment for end-to-end testing of the marketplace on
**Apple Silicon (arm64)**. Phase A stands up the *platform + data infrastructure*; application
services come in later phases.

```
Docker Desktop  →  k3d cluster (1 server + 2 agents, local registry)
                     ├── Istio (ambient or sidecar mesh) + ingress gateway
                     ├── ArgoCD (GitOps)
                     └── infra namespace:
                              PostgreSQL (catalog/inventory/order/payment DBs)
                              Kafka (Strimzi operator, KRaft single broker)
                              Redis
                              Elasticsearch (single node)
```

All images are multi-arch (arm64-native). Plain manifests are used for Postgres/Redis/ES (official
multi-arch images) to avoid fragile chart/image-hosting dependencies; Kafka uses the Strimzi operator.

## Prerequisites

- **Docker Desktop** running, with enough resources allocated in *Settings → Resources*:
  **~48 GB RAM, ~10 CPU, ~100 GB disk** (you have 128 GB, so headroom is fine).
- CLIs via Homebrew — `make prereqs` installs: `k3d kubectl helm istioctl argocd`.

## Resource budget (full SAD fidelity)

Sized for a 64GB+ machine; the cluster runs inside Docker Desktop's VM (give it ~48 GB). Rough
in-cluster footprint:

| Component | ~RAM |
|---|---|
| k3d control plane + agents | 1–2 GB |
| Istio (istiod + ingress + sidecars) | 2–3 GB |
| PostgreSQL | 0.5–1 GB |
| Kafka (Strimzi, 1 broker KRaft) | 1–1.5 GB |
| Redis | 0.3 GB |
| Elasticsearch (1 node) | 1.5–2 GB |
| ArgoCD | 0.5–1 GB |
| 6 services + sidecars (later phases) | 7–9 GB |

## Run order

```bash
make prereqs      # one-time: brew install the CLIs (Docker Desktop installed separately)
make up           # verify Docker Desktop is up, then create the k3d cluster + local registry
make istio        # install Istio + ingress gateway, label infra/marketplace ns for injection
make argocd       # install ArgoCD
make infra        # Strimzi operator + Postgres + Kafka + Redis + Elasticsearch
make observability # Prometheus + Grafana, pre-provisioned with the msfw dashboard + alert rules
make status       # show everything
```

Tear down: `make down` (delete cluster) · `make nuke` (cluster + k3d registry/volumes). Docker Desktop
itself is never touched.

## Observability

`make observability` deploys Prometheus + Grafana into the `infra` namespace:

- Prometheus discovers the marketplace pods via the `prometheus.io/*` annotations on
  `apps/*.yaml` and scrapes `/actuator/prometheus` (the services expose it on the `k8s`
  profile, with `management.metrics.tags.application` feeding the dashboard's Service selector).
- The **msfw alert rules** and the **MSFW Overview dashboard** are copies of
  `msfw/ops/observability/` — when bumping msfw, re-sync them in the same PR (that directory is
  the metric-name contract).
- Grafana runs with anonymous admin (local only): `kubectl -n infra port-forward svc/grafana 3000:3000`.

## What's NOT here yet (later phases)

- Dockerfiles / Helm charts for the 6 services (Phase B)
- Real gRPC + Kafka consumers + ES search wiring (Phase C)
- CI/CD pipelines + Istio mTLS STRICT / authz policies (Phase D)
- e2e test harness (Phase E)

> Spring Cloud Config server: in k8s we inject config via ConfigMap/Secret (k8s-native) rather than a
> standalone config server. The decision is revisited in Phase B.
