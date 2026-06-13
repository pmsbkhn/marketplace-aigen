# Runtime-layer fitness functions for the k8s manifests — the enforcement tier that scales to a
# heterogeneous estate because it governs the deployment, not the framework: it applies to any
# service regardless of language/stack. Mirrors the warn → enforce model of the code-layer rules:
#   deny  = hard requirements (build/CI fails)
#   warn  = advisory (logged, non-blocking) — the runtime equivalent of registry "warn"
#
# Run:  conftest test deploy/apps/*.yaml -p deploy/policy
package main

import rego.v1

# ---- deny: availability + supply-chain essentials -------------------------------------------

deny contains msg if {
	input.kind == "Deployment"
	some c in input.spec.template.spec.containers
	not c.resources.limits
	msg := sprintf("[deny] %s/%s: container has no resource limits (a noisy neighbour can starve the node)", [input.metadata.name, c.name])
}

deny contains msg if {
	input.kind == "Deployment"
	some c in input.spec.template.spec.containers
	endswith(c.image, ":latest")
	msg := sprintf("[deny] %s/%s: image '%s' uses :latest (non-reproducible deploys)", [input.metadata.name, c.name, c.image])
}

deny contains msg if {
	input.kind == "Deployment"
	some c in input.spec.template.spec.containers
	not contains(c.image, ":")
	msg := sprintf("[deny] %s/%s: image '%s' has no explicit tag", [input.metadata.name, c.name, c.image])
}

deny contains msg if {
	input.kind == "Deployment"
	some c in input.spec.template.spec.containers
	not c.readinessProbe
	msg := sprintf("[deny] %s/%s: no readinessProbe (traffic may hit a not-ready pod)", [input.metadata.name, c.name])
}

# ---- warn: observability + resilience (advisory) --------------------------------------------

warn contains msg if {
	input.kind == "Deployment"
	not input.spec.template.metadata.annotations["prometheus.io/scrape"]
	msg := sprintf("[warn] %s: no prometheus.io/scrape annotation — msfw metrics will not be scraped", [input.metadata.name])
}

warn contains msg if {
	input.kind == "Deployment"
	some c in input.spec.template.spec.containers
	not c.livenessProbe
	msg := sprintf("[warn] %s/%s: no livenessProbe (a wedged pod won't be restarted)", [input.metadata.name, c.name])
}
