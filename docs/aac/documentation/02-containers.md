# Containers (C4 L2)

Mỗi bounded context = **một** Spring Boot app triển khai độc lập + datastore riêng;
Kafka là trục sự kiện chung.

![Containers](embed:Containers)

| Service | Vai trò | Datastore | Tích hợp |
|---|---|---|---|
| catalog | CRUD/kiểm duyệt/tìm kiếm/giá | PostgreSQL | publish ProductCreated |
| checkout | Saga orchestrator (không DB) | in-memory | gọi REST 4 service |
| inventory | Tồn kho, reservation | PostgreSQL | consume ProductCreated, OrderCompleted |
| order | Vòng đời đơn (OMS) | PostgreSQL | publish/consume Payment & Order events |
| payment | Escrow, đối soát, payout | PostgreSQL + S3/WORM | webhook + publish/consume |
| notification | Thông báo đa kênh | PostgreSQL | consume PaymentReceived |

Chi tiết bên trong mỗi service: xem các **Component view** (`*Components`).
