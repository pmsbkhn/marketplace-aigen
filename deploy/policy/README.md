# Runtime-layer fitness functions (OPA / conftest)

The third EA enforcement tier, after the code layer (ArchUnit via `ea-archrules`) and the
contract layer (JSON fixtures). It governs the **k8s manifests**, not the framework — so it
applies to any service regardless of language or stack. This is the layer that scales to a
heterogeneous estate, because it needs no per-team framework adoption.

`kubernetes.rego` mirrors the code layer's warn → enforce split:

| Tier | Rule |
|---|---|
| **deny** (build fails) | resource limits present · no `:latest` / untagged image · readinessProbe present |
| **warn** (advisory) | `prometheus.io/scrape` annotation present (msfw metrics) · livenessProbe present |

Run: `make policy` (or `conftest test apps/*.yaml -p policy`). All current manifests pass.

At enterprise scale this same policy moves into the platform as a **Kyverno/Gatekeeper admission
policy** — caught at `kubectl apply`, independent of any service's CI. The EA registry records this
guardrail as `manifestPolicy`.
