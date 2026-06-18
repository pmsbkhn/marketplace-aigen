# ==============================================================================
# MODEL — Hạ tầng dùng chung ở tầng RUNTIME/ỨNG DỤNG (những gì code thực sự nối tới).
# Lưu ý: hạ tầng nền (Istio, Postgres, Redis, Elasticsearch, observability) được
# mô hình hoá ở deployment.dsl. Ở đây chỉ Kafka — thứ duy nhất application code
# hiện đang giao tiếp ở tầng tích hợp.
# ==============================================================================

group "Platform Infrastructure" {

    kafkaBus = container "Event Bus" "Trục sự kiện bất đồng bộ (CloudEvents/JSON). Producer = msfw transactional outbox; consumer = msfw consumption pipeline in-process. Hợp đồng wire: docs contracts/*.json." "Apache Kafka (Strimzi)" "MessageBus"
}
