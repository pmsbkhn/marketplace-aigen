# ==============================================================================
# VIEWPOINT: Operations / SRE
# Concerns: topology chạy thực tế, namespace, mesh, hạ tầng dữ liệu, observability,
#           hạ tầng đã provision nhưng app chưa wire (Redis, Elasticsearch).
# View: C4 Deployment (môi trường "Production (k3d)").
# ==============================================================================

deployment marketplaceSystem "Production (k3d)" "ProdDeployment" "Triển khai as-built trên k3d: marketplace ns (6 pod + OTel) và infra ns (Postgres, Kafka, Redis*, Elasticsearch*, observability). (*) provision nhưng chưa wire." {
    include *
    autoLayout lr
}
