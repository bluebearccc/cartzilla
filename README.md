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
    ├── product-service/ :8082  PostgreSQL   (catalog, stock consumer)
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
.\mvnw clean install -DskipTests

# 2. Bật hạ tầng + services
docker compose up --build

# 3. Thứ tự khởi động: eureka → config → services → gateway
```

Google OAuth local setup: [`docs/OAUTH_GOOGLE_SETUP.md`](docs/OAUTH_GOOGLE_SETUP.md).

SMTP email setup: [`docs/SMTP_EMAIL_SETUP.md`](docs/SMTP_EMAIL_SETUP.md).

Luồng kiểm thử xuôi/ngược và checklist compensation: [`docs/TEST-FLOWS.md`](docs/TEST-FLOWS.md).

| Thành phần | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Swagger (mỗi service) | http://localhost:8081..8085/swagger-ui.html |
| RabbitMQ | http://localhost:15672 (guest/guest) |
| MailHog | http://localhost:8025 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3100 (admin/admin) |

## Demo order flow

```powershell
docker compose up -d postgres-product postgres-order postgres-pay rabbitmq
```

| Database | Host port | DB name | User | Password |
|---|---:|---|---|---|
| Product | `${PRODUCT_DB_PORT:-5433}` | `cartzilla_product_db` | `app` | `${DB_PASSWORD:-secret}` |
| Order | `${ORDER_DB_PORT:-5434}` | `cartzilla_order_db` | `app` | `${DB_PASSWORD:-secret}` |
| Payment | `${PAYMENT_DB_PORT:-5435}` | `cartzilla_pay_db` | `app` | `${DB_PASSWORD:-secret}` |

`product-service`, `order-service`, and `payment-service` run Flyway on startup. Product has seed demo catalog in
`services/product-service/src/main/resources/db/migration/V2__seed_dev_data.sql`.

Run services from separate PowerShell terminals:

```powershell
# Terminal 1: product-service
$env:PRODUCT_DB_PORT="5433"
$env:DB_USER="app"
$env:DB_PASSWORD="secret"
$env:RABBITMQ_HOST="localhost"
.\mvnw -pl services/product-service -am spring-boot:run

# Terminal 2: payment-service
$env:PAYMENT_DB_PORT="5435"
$env:DB_USER="app"
$env:DB_PASSWORD="secret"
$env:RABBITMQ_HOST="localhost"
$env:ALWAYS_FAIL="false"
.\mvnw -pl services/payment-service -am spring-boot:run

# Terminal 3: order-service
$env:ORDER_DB_PORT="5434"
$env:DB_USER="app"
$env:DB_PASSWORD="secret"
$env:RABBITMQ_HOST="localhost"
$env:PRODUCT_SERVICE_URL="http://localhost:8082"
.\mvnw -pl services/order-service -am spring-boot:run
```

Run the demo checkout:

```powershell
.\scripts\demo-order-flow.ps1
```

The script uses seeded SKU `TSN-001-M-WHT`, creates an order through
`POST /api/orders/checkout/by-sku`, then polls `GET /api/orders/{id}/status` until the Saga finishes.

For rollback demo, restart `payment-service` with:

```powershell
$env:ALWAYS_FAIL="true"
.\mvnw -pl services/payment-service -am spring-boot:run
```

## Demo Saga rollback
Đặt `ALWAYS_FAIL=true` (env payment-service) → checkout → payment fail →
`stock.release` (compensate) → order CANCELLED → email hủy (MailHog).

## Luồng Saga checkout
```
POST /api/orders/checkout/by-sku
  → order-service calls product-service to snapshot SKU/price/name/image
  → order(PENDING) + saga(RESERVE_STOCK)
  → MQ stock.reserve  → product-service trừ kho → stock.reserved
  → MQ payment.process → payment-service → payment.result
  → order(CONFIRMED) + order.confirmed → notification gửi email
  (fail bất kỳ bước → compensate + order.cancelled)
```

## Trạng thái backend
Hoàn thiện toàn bộ feature F01–F18 theo SRS traceability:
auth + email verification + OAuth, profile/address, catalog + admin CRUD + vendor,
cart + checkout + Saga, COD (PAID khi delivered) + **VNPay thật** (create/callback có
verify chữ ký HMAC-SHA512, idempotent, amount-match), voucher validate/redeem,
staff order workflow, **notification in-app API** (`GET /api/notifications`,
`PUT /api/notifications/{id}/read`) + email log, và **admin reports** (`/api/admin/reports/*`).
Build: `.\mvnw clean install` xanh, 95 unit test pass.

> **Lưu ý chạy:** Toàn bộ các service Backend và cả Giao diện Frontend React hiện tại đã được đóng gói hoàn chỉnh bằng Dockerfile.
> Bạn chỉ cần chạy `.\mvnw clean package -DskipTests` để tạo file jar, sau đó chạy `docker compose up -d --build` để khởi động đồng bộ toàn bộ hệ thống (giao diện mở tại http://localhost:5173).
```
