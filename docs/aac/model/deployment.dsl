# ==============================================================================
# MODEL — Deployment topology (môi trường local k3d, theo deploy/).
# Đây là tầng HẠ TẦNG: phản ánh deploy/*.yaml. Lưu ý khác biệt với container view:
#   - Redis & Elasticsearch ĐÃ provision ở infra ns nhưng app CHƯA wire (Phase C)
#     -> mô hình hoá là infrastructureNode (không có containerInstance app nào).
#   - Istio cung cấp ingress gateway + service mesh (mTLS STRICT là Phase D).
#   - Mỗi pod chạy OpenTelemetry javaagent xuất trace sang Tempo.
# ==============================================================================

deploymentEnvironment "Production (k3d)" {

    deploymentNode "Developer Workstation" "Apple Silicon (arm64), Docker Desktop ~48GB" "Docker Desktop" {

        deploymentNode "k3d cluster" "1 server + 2 agents + local registry" "k3d / k3s" {

            argocd = infrastructureNode "ArgoCD" "GitOps delivery" "ArgoCD"
            istioGw = infrastructureNode "Istio Ingress Gateway" "Edge entrypoint; service mesh (mTLS STRICT: Phase D)" "Istio"

            deploymentNode "marketplace namespace" "Istio sidecar injection" "Kubernetes Namespace" {
                deploymentNode "catalog (Deployment)"      "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  { containerInstance catalogApi }
                deploymentNode "checkout (Deployment)"     "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  { containerInstance checkoutApi }
                deploymentNode "inventory (Deployment)"    "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  { containerInstance inventoryApi }
                deploymentNode "order (Deployment)"        "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  { containerInstance orderApi }
                deploymentNode "payment (Deployment)"      "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  { containerInstance paymentApi }
                deploymentNode "notification (Deployment)" "OTel javaagent -> Tempo; profile k8s" "Pod (replicas=1)"  { containerInstance notifApi }
            }

            deploymentNode "infra namespace" "" "Kubernetes Namespace" {
                deploymentNode "PostgreSQL" "catalog/inventory/order/payment/notification DBs" "StatefulSet" {
                    containerInstance catalogDb
                    containerInstance inventoryDb
                    containerInstance orderDb
                    containerInstance paymentDb
                    containerInstance notifDb
                }
                deploymentNode "Kafka" "Strimzi operator, KRaft single broker (+ schema-registry)" "Strimzi" {
                    containerInstance kafkaBus
                }
                redis = infrastructureNode "Redis" "Đã provision — app CHƯA wire (Phase C: checkout session)" "Redis"
                elastic = infrastructureNode "Elasticsearch" "Đã provision — app CHƯA wire (Phase C: catalog search)" "Elasticsearch (1 node)"
                observ = infrastructureNode "Observability" "Prometheus + Grafana + Tempo + Pushgateway" "Prometheus/Grafana/Tempo"
            }
        }
    }
}
