# Cartzilla — Fashion E-commerce Microservices

Maven multi-module monorepo · Java 21 · Spring Boot 3.5.14 · Spring Cloud 2025.0.0
Kiến trúc **DDD / Hexagonal** · Eureka + Config Server + API Gateway · RabbitMQ Saga.

> Tài liệu chi tiết: [`../docs/SRS-Cartzilla.md`](../docs/SRS-Cartzilla.md) ·
> [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md) · [`../docs/DATABASE-DESIGN.md`](../docs/DATABASE-DESIGN.md)

## Cấu trúc module

```
cartzilla/
├── pom.xml                       # parent (packaging=pom)
├── docker-compose.yml
├── shared/
│   ├── common-web/               # BaseEntity, ApiResponse, BusinessException
│   ├── common-events/            # event DTO + RabbitTopics (dùng chung MQ)
│   └── common-security/          # JwtTokenProvider
├── infra/
│   ├── eureka-server/   :8761
│   ├── config-server/   :8888
│   └── api-gateway/     :8080    # route lb://, JwtAuthFilter
└── services/
    ├── user-service/    :8081  PostgreSQL   (auth, profile, voucher)
    ├── product-service/ :8082  MongoDB      (catalog, stock consumer)
    ├── order-service/   :8083  PostgreSQL   (cart, order, Saga orchestrator)
    ├── payment-service/ :8084  PostgreSQL   (COD/VNPay)
    └── notification-service/ :8085 PostgreSQL (email consumer)
```

## Kiến trúc 1 service (Hexagonal)

```
api/            controller · dto · exception     (inbound adapter)
application/    command · usecase                (orchestration)
domain/         aggregate · entity · vo · repository(port) · exception
infrastructure/ adapter · persistence · messaging · saga   (outbound adapter)
```
Dependency: `api → application → domain ← infrastructure` (domain không phụ thuộc framework).

## Chạy local

```bash
# 1. Build toàn bộ
mvn clean install -DskipTests

# 2. Bật hạ tầng + services
docker compose up --build

# 3. Thứ tự khởi động: eureka → config → services → gateway
```

| Thành phần | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Swagger (mỗi service) | http://localhost:8081..8085/swagger-ui.html |
| RabbitMQ | http://localhost:15672 (guest/guest) |
| MailHog | http://localhost:8025 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3100 (admin/admin) |

## Demo Saga rollback
Đặt `ALWAYS_FAIL=true` (env payment-service) → checkout → payment fail →
`stock.release` (compensate) → order CANCELLED → email hủy (MailHog).

## Luồng Saga checkout
```
POST /api/orders/checkout
  → order(PENDING) + saga(RESERVE_STOCK)
  → MQ stock.reserve  → product-service trừ kho → stock.reserved
  → MQ payment.process → payment-service → payment.result
  → order(CONFIRMED) + order.confirmed → notification gửi email
  (fail bất kỳ bước → compensate + order.cancelled)
```

## Trạng thái skeleton
Đã có: cấu trúc đầy đủ, auth (register/login + JWT), catalog list + stock consumer,
checkout + Saga orchestrator hoàn chỉnh, payment COD mock, email consumer.
TODO mở rộng: address/voucher CRUD, admin catalog, cart CRUD, staff orders,
VNPay thật, reports, frontend React (`../frontend`).
```
