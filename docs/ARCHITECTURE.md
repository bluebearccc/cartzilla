# Architecture — Cartzilla Microservices

> **Lưu ý quan trọng:** Tài liệu này giữ nguyên **chức năng nghiệp vụ Cartzilla** (e-commerce thời trang) nhưng **tổ chức folder/structure theo đúng codebase thực tế** của repo `SE1911-JV_MSS301` (TalentHub skeleton): Maven multi-module monorepo · Eureka + Config Server + API Gateway · shared modules · kiến trúc **DDD / Hexagonal (Ports & Adapters)**.

**Stack thực tế (theo `pom.xml` repo):**
- Java 21 · Spring Boot **3.5.14** · Spring Cloud **2025.0.0** · Lombok 1.18.46 · Maven multi-module
- Service Discovery: **Spring Cloud Netflix Eureka** (port 8761)
- Centralized Config: **Spring Cloud Config Server** (port 8888)
- API Gateway: **Spring Cloud Gateway** (port 8080)
- Persistence: Spring Data JPA + PostgreSQL 16 cho tất cả domain services, gồm cả catalog/product-service
- Package root: `com.cartzilla.<service>` (mirror `com.talenthub.<service>` của skeleton)

---

## 1. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                 Frontend — React 18 + TS :5173                  │
│         Vite · TanStack Query · React Context · Axios           │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP REST
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              infra/api-gateway  :8080  (Spring Cloud Gateway)    │
│   GlobalFilter: JwtAuthFilter · CORS · RateLimiter · Logging    │
│   Route resolve qua Eureka: lb://USER-SERVICE, lb://PRODUCT...   │
└───┬───────────────┬───────────────┬───────────────┬─────────────┘
    │               │               │               │
    │   ┌───────────┴───────────────┴───────────────┴───────────┐
    │   │   infra/eureka-server :8761  (Service Registry)        │
    │   │   ← tất cả service register & discover qua đây         │
    │   └────────────────────────────────────────────────────────┘
    │   ┌────────────────────────────────────────────────────────┐
    │   │   infra/config-server :8888  (Centralized Config)       │
    │   │   ← serve application.yml cho mọi service               │
    │   └────────────────────────────────────────────────────────┘
    │
    ▼            ▼              ▼              ▼              ▼
┌────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────────┐
│ user-  │ │ product- │ │  order-  │ │ payment- │ │  notification-  │
│service │ │ service  │ │ service  │ │ service  │ │    service      │
│ :8081  │ │  :8082   │ │  :8083   │ │  :8084   │ │     :8085       │
│        │ │          │ │  +Saga   │ │          │ │   (email)       │
│postgres│ │ postgres │ │ postgres │ │ postgres │ │    postgres     │
│user_db │ │product_db│ │ order_db │ │payment_db│ │ notification_db │
└────────┘ └──────────┘ └──────────┘ └──────────┘ └─────────────────┘
    │           │            │            │              │
    └───────────┴────────────┴────────────┴──────────────┘
                            │
                            ▼
            ┌───────────────────────────┐
            │      RabbitMQ :5672       │   ← shared/common-events
            │      Mgmt UI :15672       │      định nghĩa event DTO
            │  stock / payment / order  │
            └───────────────────────────┘
            ┌───────────────────────────┐
            │   MailHog :8025 (dev SMTP)│
            └───────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Prometheus :9090 + Grafana :3100 ← /actuator/prometheus mỗi svc │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Maven Module Layout (theo repo thực tế)

Parent POM: `com.cartzilla:cartzilla-parent:1.0-SNAPSHOT` (`packaging=pom`), giống `talenthub-parent`.

```
cartzilla-parent (pom)
├── shared/
│   ├── common-events      ← event DTO dùng chung qua RabbitMQ
│   ├── common-web         ← BaseEntity, ApiResponse, exception base
│   └── common-security    ← JwtTokenProvider, SecurityConfig dùng chung
├── infra/
│   ├── eureka-server      ← Service Registry  (:8761)
│   ├── config-server      ← Config Server     (:8888)
│   └── api-gateway        ← Spring Cloud Gateway (:8080)
└── services/
    ├── user-service       ← :8081  (PostgreSQL)
    ├── product-service    ← :8082  (PostgreSQL)
    ├── order-service      ← :8083  (PostgreSQL) + Saga
    ├── payment-service    ← :8084  (PostgreSQL)
    └── notification-service ← :8085 (PostgreSQL + email)
```

**Mapping với service gốc của repo (đổi service, giữ structure):**

| Repo gốc (TalentHub) | → Cartzilla (đổi sang) | Vai trò |
|---|---|---|
| `job-service` | `product-service` | Catalog sản phẩm |
| `candidate-service` | `user-service` | Tài khoản, profile, địa chỉ, voucher |
| `application-service` | `order-service` | Giỏ hàng, đơn hàng, Saga |
| `cv-parser-service` | `payment-service` | Thanh toán COD/VNPay |
| `notification-service` | `notification-service` | Email (giữ nguyên tên) |
| `infra/eureka-server` | `infra/eureka-server` | Giữ nguyên |
| `infra/config-server` | `infra/config-server` | Giữ nguyên |
| `infra/api-gateway` | `infra/api-gateway` | Giữ nguyên |
| `shared/common-*` | `shared/common-*` | Giữ nguyên |

---

## 3. Kiến trúc nội bộ 1 Service — DDD / Hexagonal (Ports & Adapters)

Theo đúng cấu trúc `job-service` / `candidate-service` trong repo:

```
<service>/src/main/java/com/cartzilla/<service>/
│
├── <Service>Application.java          ← @SpringBootApplication @EnableDiscoveryClient
│
├── api/                               ← INBOUND ADAPTER (REST layer)
│   ├── controller/
│   │   └── XxxController.java         ← @RestController, gọi usecase
│   ├── dto/
│   │   ├── XxxCreateRequest.java      ← request DTO, có .toCommand()
│   │   └── XxxResponse.java           ← response DTO, có static .from(entity)
│   ├── exception/
│   │   └── GlobalExceptionHandler.java ← @RestControllerAdvice
│   ├── ApiPaths.java                  ← hằng số path
│   └── PingController.java            ← health/ping (có sẵn trong skeleton)
│
├── application/                       ← USE CASE layer (orchestration)
│   ├── command/
│   │   └── XxxCommand.java            ← input command (record lồng nhau)
│   └── usecase/
│       └── DoSomethingUseCase.java    ← @Service @Transactional, 1 usecase/class
│
├── domain/                           ← DOMAIN layer (pure business, no framework)
│   ├── aggregate/
│   │   └── XxxAggregate.java          ← aggregate root, factory create()
│   ├── entity/
│   │   └── Xxx.java                   ← @Entity, private ctor + static create()
│   ├── vo/
│   │   └── Xxx.java                   ← @Embeddable value object (vd SalaryRange→Money)
│   ├── repository/
│   │   └── XxxRepository.java         ← PORT (interface, domain định nghĩa)
│   └── exception/
│       └── XxxException.java          ← domain exception
│
└── infrastructure/                   ← OUTBOUND ADAPTER
    ├── adapter/
    │   └── XxxRepositoryAdapter.java  ← implements domain port, map domain↔jpa
    ├── persistence/
    │   └── XxxJpaRepository.java      ← extends JpaRepository (Spring Data)
    └── specification/
        └── XxxSearchCriteria.java     ← JPA Specification cho filter/search
```

**Luồng gọi trong 1 service (1 chiều, theo Hexagonal):**

```
HTTP Request
   ↓
api/controller  ──(request.toCommand())──►  application/usecase
   ↓                                              ↓
api/dto.Response ◄──(Response.from(entity))──  domain/aggregate + domain/entity
                                                  ↓ (qua PORT)
                                          domain/repository (interface)
                                                  ↓ (implements)
                                          infrastructure/adapter
                                                  ↓
                                          infrastructure/persistence (JpaRepository)
                                                  ↓
                                              Database
```

> **Nguyên tắc dependency:** `api → application → domain ← infrastructure`. Domain KHÔNG phụ thuộc framework/Spring. Infrastructure implements port của domain (Dependency Inversion).

---

## 4. Convention code (trích từ repo thực tế)

### 4.1 Domain Entity — factory pattern (theo `Job.java`)

```java
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @Embedded private Money basePrice;  // value object
    private ProductStatus status;

    public static Product create(String name, Money basePrice) {
        Product p = new Product();
        p.name = name;
        p.basePrice = basePrice;
        p.status = ProductStatus.ACTIVE;
        return p;
    }
}
```

### 4.2 Aggregate root (theo `JobAggregate.java`)

```java
@Getter
public class OrderAggregate {
    private final Order order;
    private final List<OrderItem> items;

    private OrderAggregate(Order order, List<OrderItem> items) { ... }

    public static OrderAggregate create(UUID userId, List<CartLine> lines, Address addr) {
        Order order = Order.create(userId, addr);
        List<OrderItem> items = lines.stream().map(l -> OrderItem.create(order.getId(), l)).toList();
        return new OrderAggregate(order, items);
    }
}
```

### 4.3 UseCase (theo `CreateNewJobUseCase.java`)

```java
@Service
@RequiredArgsConstructor
public class CheckoutUseCase {
    private final OrderRepository orderRepository;   // domain port
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Transactional
    public Order execute(OrderCommand.Checkout cmd) {
        OrderAggregate aggregate = OrderAggregate.create(cmd.userId(), cmd.lines(), cmd.address());
        Order order = orderRepository.save(aggregate);
        sagaOrchestrator.start(order);
        return order;
    }
}
```

### 4.4 Controller (theo `JobController.java`)

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final CheckoutUseCase checkoutUseCase;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestBody @Valid CheckoutRequest req) {
        Order order = checkoutUseCase.execute(req.toCommand());
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

### 4.5 Port + Adapter (theo `CandidateRepository` + `CandidateRepositoryAdapter`)

```java
// domain/repository/OrderRepository.java  — PORT
public interface OrderRepository {
    Order save(OrderAggregate aggregate);
    Optional<Order> findById(UUID id);
}

// infrastructure/adapter/OrderRepositoryAdapter.java  — ADAPTER
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository jpa;       // Spring Data
    @Override public Order save(OrderAggregate a) { ... map & jpa.save ... }
}
```

---

## 5. Service Discovery + Config (Eureka + Config Server)

### 5.1 Mỗi service là Eureka client (theo `job-service/application.yml`)

```yaml
server:
  port: 8082                            # product-service
spring:
  application:
    name: product-service               # tên đăng ký Eureka
  config:
    import: optional:configserver:http://localhost:8888   # lấy config tập trung
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### 5.2 Gateway route qua Eureka (load-balanced `lb://`)

```yaml
# infra/api-gateway/application.yml
server:
  port: 8080
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true                 # tự discover service qua Eureka
      routes:
        - id: user-service
          uri: lb://USER-SERVICE        # ← lb:// resolve qua Eureka, KHÔNG hardcode host
          predicates: [ Path=/api/users/**, /api/vouchers/** ]
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates: [ Path=/api/products/** ]
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates: [ Path=/api/orders/**, /api/staff/orders/** ]
          filters: [ JwtAuthFilter ]
        - id: payment-service
          uri: lb://PAYMENT-SERVICE
          predicates: [ Path=/api/payments/** ]
          filters: [ JwtAuthFilter ]
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:5173"
            allowedMethods: "*"
            allowedHeaders: "*"
```

> So với Cartzilla plan gốc (đề xuất bỏ Eureka, dùng Docker DNS): repo thực tế **dùng Eureka**, nên kiến trúc này theo repo — route bằng `lb://SERVICE-NAME` thay vì `http://host:port`.

---

## 6. Inter-Service Communication

### 6.1 Synchronous (REST qua Gateway/Eureka — dùng khi cần response ngay)

```
order-service ──(POST lb://USER-SERVICE/api/vouchers/validate)──► user-service
                  (preview voucher khi checkout, chưa tăng usedCount)
order-service ──(POST lb://USER-SERVICE/api/vouchers/redeem)────► user-service
                  (commit voucher sau checkout/payment thành công)
gateway       ──(validate JWT)──► dùng common-security (JwtTokenProvider)
```
Gọi qua `WebClient`/`RestClient` với `@LoadBalanced` (đã có `HttpClientConfig.java` trong job-service làm mẫu).

### 6.2 Asynchronous (RabbitMQ — event DTO trong `shared/common-events`)

```
order-service   ─publish→ stock.exchange/stock.reserve    →  product-service
product-service ─publish→ stock.exchange/stock.reserved   →  order-service
order-service   ─publish→ payment.exchange/payment.process →  payment-service
payment-service ─publish→ payment.exchange/payment.result  →  order-service
order-service   ─publish→ order.exchange/order.confirmed   →  notification-service
order-service   ─publish→ order.exchange/order.cancelled   →  notification-service
order-service   ─publish→ stock.exchange/stock.release      →  product-service (compensate)
```

> Tất cả event class (`StockReserveEvent`, `PaymentResultEvent`, …) đặt trong **`shared/common-events`** (`com.cartzilla.events`) để cả publisher và consumer cùng dùng — không duplicate DTO.

---

## 7. Saga Orchestration — Checkout Flow

Saga orchestrator đặt tại `order-service/.../order/saga/` (tương ứng layer `application` + một outbound adapter messaging).

```
Customer  POST /api/orders/checkout
   │ (CheckoutUseCase → OrderSagaOrchestrator.start)
   ▼
[1] order-service: tạo Order(PENDING) + SagaState(RESERVE_STOCK, IN_PROGRESS)
                   publish → stock.reserve
   ▼
[2] product-service: StockConsumer → kiểm tra & trừ tồn kho
                   publish → stock.reserved {success}
   ▼
   ├─ success → order-service: SagaState=PROCESS_PAYMENT; publish → payment.process
   └─ fail    → order-service: Order=CANCELLED; Saga=FAILED; publish → order.cancelled
   ▼
[3] payment-service: PaymentConsumer → COD mock OK / VNPay redirect+callback
                   publish → payment.result {success}
   ▼
   ├─ success → order-service: Order=CONFIRMED; SagaState=DONE; publish → order.confirmed
   └─ fail    → order-service: publish → stock.release (compensate);
                              Order=CANCELLED; Saga=FAILED; publish → order.cancelled
   ▼
[4] notification-service: NotificationConsumer (order.confirmed/cancelled) → gửi email
```

State machine `SagaState`: `RESERVE_STOCK → PROCESS_PAYMENT → NOTIFY → DONE` | nhánh lỗi → `FAILED` + compensation.

---

## 8. Full Folder Structure (mở rộng, theo repo)

```
SE1911-JV_MSS301/
├── pom.xml                              ← parent (packaging=pom, liệt kê modules)
├── docker-compose.infra.yml            ← network + infra (mở rộng từ file repo)
├── docker-compose.yml                  ← full stack (bổ sung)
├── README.md
├── docs/
│   ├── SRS-Cartzilla.md
│   ├── DBDesign_Cartzilla.md
│   ├── DomainModel_Cartzilla.md
│   └── ARCHITECTURE.md
│
├── shared/
│   ├── common-events/                  ← com.cartzilla.events
│   │   └── src/main/java/com/cartzilla/events/
│   │       ├── stock/  StockReserveEvent · StockReservedEvent · StockReleaseEvent
│   │       ├── payment/ PaymentProcessEvent · PaymentResultEvent
│   │       └── order/  OrderConfirmedEvent · OrderCancelledEvent
│   ├── common-web/                     ← com.cartzilla.web
│   │   └── src/main/java/com/cartzilla/web/
│   │       ├── base/BaseEntity.java    ← @MappedSuperclass createdAt/updatedAt
│   │       ├── response/ApiResponse.java
│   │       └── exception/BusinessException.java
│   └── common-security/                ← com.cartzilla.security
│       └── src/main/java/com/cartzilla/security/
│           ├── JwtTokenProvider.java
│           └── SecurityConfig.java
│
├── infra/
│   ├── eureka-server/                  ← :8761  @EnableEurekaServer
│   ├── config-server/                  ← :8888  @EnableConfigServer
│   └── api-gateway/                    ← :8080  com.cartzilla.gateway
│       └── src/main/java/com/cartzilla/gateway/
│           ├── ApiGatewayApplication.java
│           ├── filter/JwtAuthFilter.java
│           └── config/CorsConfig.java
│
└── services/
    ├── user-service/                   ← :8081  com.cartzilla.user
    │   └── src/main/java/com/cartzilla/user/
    │       ├── UserServiceApplication.java
    │       ├── api/
    │       │   ├── controller/  AuthController · UserController · AddressController
    │       │   │                AdminUserController · VoucherController
    │       │   ├── dto/  RegisterRequest · LoginRequest · LoginResponse · ...
    │       │   ├── exception/GlobalExceptionHandler.java
    │       │   └── ApiPaths.java
    │       ├── application/
    │       │   ├── command/  AuthCommand · VoucherCommand
    │       │   └── usecase/  RegisterUserUseCase · LoginUseCase ·
    │       │                 RefreshTokenUseCase · ValidateVoucherUseCase · RedeemVoucherUseCase
    │       ├── domain/
    │       │   ├── aggregate/UserAggregate.java
    │       │   ├── entity/  User · Address · RefreshToken · Voucher · VoucherUsage · VoucherAllowedUser
    │       │   ├── vo/  Email · Role
    │       │   ├── repository/  UserRepository · VoucherRepository
    │       │   └── exception/DuplicateEmailException.java
    │       └── infrastructure/
    │           ├── adapter/UserRepositoryAdapter · VoucherRepositoryAdapter
    │           └── persistence/UserJpaRepository · VoucherJpaRepository
    │   └── src/main/resources/
    │       ├── application.yml
    │       └── db/migration/V1__init.sql       ← Flyway
    │
    ├── product-service/                ← :8082  com.cartzilla.product  (PostgreSQL)
    │   └── src/main/java/com/cartzilla/product/
    │       ├── api/controller/  ProductController · CategoryController · AdminProductController
    │       ├── application/usecase/  ListProductsUseCase · GetProductUseCase ·
    │       │                         CreateProductUseCase · ReserveStockUseCase
    │       ├── domain/
    │       │   ├── aggregate/ProductAggregate.java
    │       │   ├── entity/  Product · Category · Vendor · ProductVariant · ProductImage
    │       │   ├── vo/  Money · Slug · Sku · VendorType · ColorHex
    │       │   └── repository/  ProductRepository · CategoryRepository · VendorRepository
    │       └── infrastructure/
    │           ├── adapter/ProductRepositoryAdapter
    │           ├── persistence/ProductJpaRepository · CategoryJpaRepository · VendorJpaRepository
    │           └── messaging/StockConsumer · StockPublisher
    │   └── src/main/resources/
    │       ├── application.yml
    │       └── db/migration/V1__init.sql       ← Flyway
    │
    ├── order-service/                  ← :8083  com.cartzilla.order
    │   └── src/main/java/com/cartzilla/order/
    │       ├── api/controller/  CartController · OrderController · StaffOrderController
    │       ├── application/
    │       │   ├── command/  CartCommand · OrderCommand
    │       │   └── usecase/  AddToCartUseCase · CheckoutUseCase ·
    │       │                 ViewOrdersUseCase · UpdateOrderStatusUseCase
    │       ├── domain/
    │       │   ├── aggregate/  CartAggregate · OrderAggregate
    │       │   ├── entity/  CartItem · Order · OrderItem · OrderStatusLog · SagaState
    │       │   ├── vo/  ShippingAddress · OrderStatus · Money
    │       │   └── repository/  CartRepository · OrderRepository · SagaStateRepository
    │       ├── infrastructure/
    │       │   ├── adapter/  CartRepositoryAdapter · OrderRepositoryAdapter
    │       │   ├── persistence/  CartJpaRepository · OrderJpaRepository · SagaStateJpaRepository
    │       │   ├── specification/OrderSearchCriteria.java   ← filter staff orders
    │       │   ├── messaging/  SagaConsumer · SagaPublisher
    │       │   ├── saga/OrderSagaOrchestrator.java
    │       │   └── scheduler/OrderCancellationJob.java      ← BR03 hủy COD 48h
    │
    ├── payment-service/                ← :8084  com.cartzilla.payment
    │   └── src/main/java/com/cartzilla/payment/
    │       ├── api/controller/PaymentController.java        ← VNPay callback
    │       ├── application/usecase/ProcessPaymentUseCase · HandleVnpayCallbackUseCase
    │       ├── domain/
    │       │   ├── entity/Payment.java
    │       │   ├── vo/  PaymentMethod · PaymentStatus
    │       │   └── repository/PaymentRepository
    │       └── infrastructure/
    │           ├── adapter/PaymentRepositoryAdapter
    │           ├── persistence/PaymentJpaRepository
    │           └── messaging/PaymentConsumer · PaymentPublisher
    │
    └── notification-service/           ← :8085  com.cartzilla.notification
        └── src/main/java/com/cartzilla/notification/
            ├── application/usecase/SendOrderEmailUseCase
            ├── domain/
            │   ├── entity/NotificationLog.java
            │   └── repository/NotificationLogRepository
            └── infrastructure/
                ├── adapter/NotificationLogRepositoryAdapter
                ├── persistence/NotificationLogJpaRepository
                ├── messaging/NotificationConsumer        ← order.confirmed/cancelled
                └── email/EmailService.java                ← Spring Mail + Thymeleaf
        └── src/main/resources/templates/email/
            ├── order-confirmed.html
            └── order-cancelled.html
```

---

## 9. RabbitMQ — Event Topology

```
Topic Exchanges:   stock.exchange · payment.exchange · order.exchange

Routing key → Queue → Consumer:
  stock.reserve   → stock.reserve.queue   → product-service.StockConsumer
  stock.reserved  → stock.reserved.queue  → order-service.SagaConsumer
  stock.release   → stock.release.queue   → product-service.StockConsumer
  payment.process → payment.process.queue → payment-service.PaymentConsumer
  payment.result  → payment.result.queue  → order-service.SagaConsumer
  order.confirmed → order.confirmed.queue → notification-service.NotificationConsumer
  order.cancelled → order.cancelled.queue → notification-service.NotificationConsumer
```

Config `RabbitConfig` (exchange/queue/binding + `Jackson2JsonMessageConverter`) đặt trong `infrastructure/messaging/` của mỗi service. Event DTO ở `shared/common-events`.

---

## 10. JWT Auth Flow (dùng `shared/common-security`)

```
[FE] POST /api/users/login
   → user-service.LoginUseCase: verify (bcrypt) → JwtTokenProvider.generate
   → { accessToken(15m), refreshToken(7d), user }
[FE] lưu localStorage; request sau: Authorization: Bearer <token>
   → api-gateway.JwtAuthFilter: JwtTokenProvider.validate
       ✓ → inject header X-User-Id, X-User-Role → forward downstream
       ✗ → 401
[FE] khi 401 → axios interceptor → POST /api/users/refresh-token → token mới
```

`JwtTokenProvider` đặt ở `common-security` → gateway và user-service cùng import (không duplicate logic ký/verify).

---

## 11. Resilience4j — Circuit Breaker

```java
// order-service gọi user-service validate voucher
@CircuitBreaker(name = "user-service", fallbackMethod = "voucherFallback")
public VoucherResult validate(String code, UUID userId, BigDecimal amount) { ... }

public VoucherResult voucherFallback(String code, UUID userId, BigDecimal amount, Throwable t) {
    return VoucherResult.validationUnavailable(); // nếu user nhập voucher thì checkout trả lỗi validate voucher
}
```
```yaml
resilience4j.circuitbreaker.instances.user-service:
  failure-rate-threshold: 50
  wait-duration-in-open-state: 10s
  sliding-window-size: 5
```

---

## 12. Observability

Mỗi service thêm `spring-boot-starter-actuator` + `micrometer-registry-prometheus`:
```yaml
management.endpoints.web.exposure.include: health, info, prometheus, metrics
management.endpoint.health.show-details: always
```
Prometheus scrape `/actuator/prometheus` từng service · Grafana import dashboard JVM Micrometer (ID 4701).

---

## 13. Ports tổng hợp

| Module | Port | Loại |
|---|---|---|
| frontend | 5173 | React/Vite |
| infra/api-gateway | 8080 | Gateway |
| infra/eureka-server | 8761 | Service Registry |
| infra/config-server | 8888 | Config |
| user-service | 8081 | PostgreSQL |
| product-service | 8082 | PostgreSQL |
| order-service | 8083 | PostgreSQL + Saga |
| payment-service | 8084 | PostgreSQL |
| notification-service | 8085 | PostgreSQL + email |
| RabbitMQ | 5672 / 15672 | Messaging |
| MailHog | 1025 / 8025 | SMTP dev |
| Prometheus | 9090 | Metrics |
| Grafana | 3100 | Dashboard |

---

## 14. Inter-Service API/Event Contracts

| # | Luồng | Kiểu | Producer | Consumer | Payload |
|---|---|---|---|---|---|
| 1 | Validate voucher preview | REST | order-service | user-service | `{code,userId,amount}` → `{valid,discount,voucherId,normalizedCode,reasonCode}` |
| 1b | Redeem voucher commit | REST | order-service | user-service | `{voucherId,code,userId,orderId,amount}` → `{redeemed,discount,usageId}` |
| 2 | Reserve stock | MQ | order-service | product-service | `StockReserveEvent{orderId,items[]}` |
| 3 | Stock result | MQ | product-service | order-service | `StockReservedEvent{orderId,success,failedSku}` |
| 4 | Process payment | MQ | order-service | payment-service | `PaymentProcessEvent{orderId,amount,method}` |
| 5 | Payment result | MQ | payment-service | order-service | `PaymentResultEvent{orderId,success,txnId}` |
| 6 | Order confirmed | MQ | order-service | notification-service | `OrderConfirmedEvent{orderId,email}` |
| 7 | Order cancelled | MQ | order-service | notification-service | `OrderCancelledEvent{orderId,reason,email}` |
| 8 | JWT claims | Header | api-gateway | all services | `X-User-Id`, `X-User-Role` |

---

*Cartzilla Architecture v2.0 — DDD/Hexagonal · Eureka + Config Server + Gateway · aligned với repo SE1911-JV_MSS301*
