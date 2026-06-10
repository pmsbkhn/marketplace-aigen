# Local k8s platform for marketplace e2e (Phase A). Apple Silicon / arm64.
# Usage: make help

CLUSTER       ?= marketplace
ISTIO_NS      ?= istio-system
INFRA_NS      ?= infra
APP_NS        ?= marketplace
K           := kubectl

.DEFAULT_GOAL := help

.PHONY: help prereqs docker-check up istio argocd strimzi infra status down nuke namespaces

help: ## Show targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

prereqs: ## One-time: install CLIs via Homebrew (Docker Desktop installed separately)
	brew install k3d kubectl helm istioctl argocd

docker-check: ## Verify the Docker Desktop daemon is running with enough resources
	@docker info >/dev/null 2>&1 || { echo "Docker Desktop is not running. Start it, then:"; \
		echo "  Settings -> Resources: ~48 GB RAM, ~10 CPU, ~100 GB disk"; exit 1; }
	@echo "Docker daemon: UP  ($$(docker info --format '{{.MemTotal}}' | awk '{printf \"%.0f GB RAM allocated\", $$1/1073741824}'))"

up: docker-check ## Create the k3d cluster + local registry (on Docker Desktop)
	k3d cluster create --config cluster/k3d-cluster.yaml
	$(K) cluster-info

namespaces: ## Create infra + app namespaces
	$(K) create namespace $(INFRA_NS) --dry-run=client -o yaml | $(K) apply -f -
	$(K) create namespace $(APP_NS)   --dry-run=client -o yaml | $(K) apply -f -

istio: namespaces ## Install Istio + ingress gateway, enable injection on the app ns only
	istioctl install -y -f istio/istio-values.yaml
	# Mesh the application services. Infra (Postgres/Kafka/Redis/ES) stays OUT of the mesh —
	# Istio sidecars interfere with Kafka broker ports; app sidecars reach infra over plaintext.
	$(K) label namespace $(APP_NS) istio-injection=enabled --overwrite
	$(K) -n $(ISTIO_NS) rollout status deploy/istiod

argocd: ## Install ArgoCD (GitOps)
	$(K) create namespace argocd --dry-run=client -o yaml | $(K) apply -f -
	$(K) apply --server-side --force-conflicts -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
	$(K) -n argocd rollout status deploy/argocd-server --timeout=300s
	@echo "ArgoCD admin password:" && $(K) -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d; echo
	@echo "Port-forward: kubectl -n argocd port-forward svc/argocd-server 8090:443"

strimzi: namespaces ## Install the Strimzi (Kafka) operator into the infra namespace
	helm repo add strimzi https://strimzi.io/charts/ && helm repo update
	helm upgrade --install strimzi strimzi/strimzi-kafka-operator \
		-n $(INFRA_NS) --set watchNamespaces="{$(INFRA_NS)}" --wait

infra: namespaces strimzi ## Deploy Postgres + Kafka + Redis + Elasticsearch
	$(K) apply -n $(INFRA_NS) -f infra/postgres.yaml
	$(K) apply -n $(INFRA_NS) -f infra/redis.yaml
	$(K) apply -n $(INFRA_NS) -f infra/elasticsearch.yaml
	$(K) apply -n $(INFRA_NS) -f infra/kafka-strimzi.yaml
	$(K) -n $(INFRA_NS) wait --for=condition=ready pod -l app=postgres --timeout=300s
	$(K) -n $(INFRA_NS) wait kafka/marketplace --for=condition=Ready --timeout=600s || true
	@echo "infra deployed in namespace '$(INFRA_NS)'"

status: ## Show cluster + platform status
	@echo "== nodes =="     && $(K) get nodes
	@echo "== istio =="     && $(K) -n $(ISTIO_NS) get pods
	@echo "== argocd =="    && $(K) -n argocd get pods 2>/dev/null | head
	@echo "== infra =="     && $(K) -n $(INFRA_NS) get pods
	@echo "== marketplace ==" && $(K) -n $(APP_NS) get pods 2>/dev/null

down: ## Delete the k3d cluster (Docker Desktop keeps running)
	k3d cluster delete $(CLUSTER)

nuke: down ## Delete cluster + prune the k3d registry/volumes (Docker Desktop itself is untouched)
	-k3d registry delete marketplace-registry
	-docker volume prune -f
