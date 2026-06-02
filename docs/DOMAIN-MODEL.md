# Domain Model — Cartzilla (Aggregate · Entity · Value Object)

> Xác định **Aggregate Root**, **Entity**, **Value Object (VO)** cho tất cả 5 service theo DDD/Hexagonal.
> Quy ước: **AR** = Aggregate Root · **E** = Entity (trong aggregate) · **VO** = Value Object (bất biến, không có ID).
> Mỗi service = 1 **Bounded Context** · DB riêng · không tham chiếu trực tiếp qua aggregate của context khác (chỉ qua ID hoặc event).

---

## 0. Nguyên tắc phân loại

| Loại | Đặc điểm | Annotation/Code |
|---|---|---|
| **Aggregate Root** | Cổng vào duy nhất, đảm bảo invariant, transaction boundary | `@Entity` + factory `create()`, kế thừa `BaseEntity` |
| **Entity** | Có identity riêng nhưng sống trong vòng đời của AR | `@Entity`, được AR quản lý (cascade) |
| **Value Object** | Bất biến, so sánh bằng giá trị, không có ID | `@Embeddable` (JPA) / class thường / `enum` / `record` |

**Quy tắc tham chiếu giữa aggregate:** tham chiếu **bằng ID** (UUID / String), không nhúng aggregate khác. VD `Order` giữ `userId` chứ không giữ object `User`.

---

## 1. user-service — Context: Identity & Account

| Tên | Loại | Vai trò |
|---|---|---|
| **User** | **AR** | Tài khoản đăng nhập; gốc cho Address + RefreshToken |
| Address | E | Địa chỉ giao hàng thuộc User |
| RefreshToken | E | Token làm mới phiên đăng nhập |
| **Voucher** | **AR** | Mã giảm giá (aggregate độc lập) |
| VoucherUsage | E | Lượt sử dụng voucher của 1 user/order |
| Role | VO (enum) | `CUSTOMER · STAFF · ADMIN` |
| Email | VO | Email hợp lệ (định dạng, chuẩn hoá lowercase) |
| DiscountPolicy | VO | `type (PERCENTAGE/FIXED)` + `value` + `minOrderAmount` |

```
UserAggregate (AR: User)
 ├── Address       (E, 0..*)
 └── RefreshToken  (E, 0..*)
 VO: Email, Role

VoucherAggregate (AR: Voucher)
 └── VoucherUsage  (E, 0..*)
 VO: DiscountPolicy
```

**Invariant chính:**
- User.email là duy nhất trong context (đã có `DuplicateEmailException`).
- Voucher: `usedCount ≤ maxUses`; chỉ valid khi `isActive && now < expiresAt && orderAmount ≥ minOrderAmount`.

**Trạng thái code hiện tại:** đã có `User` (AR), `Role` (VO). **Cần bổ sung:** `Address`, `RefreshToken`, `Voucher`, `VoucherUsage`, VO `Email`, `DiscountPolicy`.

---

## 2. product-service — Context: Catalog (MongoDB)

| Tên | Loại | Vai trò |
|---|---|---|
| **Product** | **AR** | Sản phẩm + danh sách biến thể; gốc cho mọi thao tác tồn kho |
| **Category** | **AR** | Danh mục (cây), aggregate độc lập |
| ProductVariant | VO | Biến thể size/màu, có `sku`, `stock`, `price` (embedded) |
| Money | VO | Số tiền + đơn vị (chuẩn hoá BigDecimal) |
| ProductStatus | VO (enum) | `ACTIVE · INACTIVE · DELETED` |
| Sku | VO | Mã biến thể (định dạng `<TÊN>-<MÀU>-<SIZE>`) |

```
ProductAggregate (AR: Product)
 └── ProductVariant (VO, 1..*, embedded trong document)
 VO: Money, ProductStatus, Sku

CategoryAggregate (AR: Category)
 VO: slug, parentId (tham chiếu bằng ID)
```

**Invariant chính:**
- `ProductVariant.stock ≥ 0` luôn đúng (khấu trừ kho phải qua method của Product AR, không set thẳng).
- SKU là duy nhất trong 1 Product; mọi reserve/release đi qua `ProductAggregate`.

> Vì MongoDB schema-less: variant nhúng trực tiếp trong document `products` (đúng tinh thần VO trong aggregate, không cần collection riêng).

**Trạng thái code hiện tại:** đã có `Product` (AR), `ProductVariant` (VO). **Cần bổ sung:** `Category` (AR), VO `Money`, `ProductStatus`, `Sku`; chuyển logic trừ kho vào method trong `Product`.

---

## 3. order-service — Context: Ordering & Saga (giàu domain nhất)

| Tên | Loại | Vai trò |
|---|---|---|
| **Cart** | **AR** | Giỏ hàng server-side của 1 user |
| CartItem | E | Dòng sản phẩm trong giỏ |
| **Order** | **AR** | Đơn hàng — transaction boundary chính |
| OrderItem | E | Dòng hàng (snapshot tại thời điểm đặt) |
| OrderStatusLog | E | Lịch sử đổi trạng thái (audit) |
| **SagaState** | **AR** | Trạng thái distributed transaction (aggregate riêng) |
| OrderStatus | VO (enum) | `PENDING · CONFIRMED · SHIPPING · DELIVERED · CANCELLED` |
| PaymentMethod | VO (enum) | `COD · VNPAY` |
| Money | VO | subtotal / discount / total |
| ShippingAddress | VO | Snapshot địa chỉ (BR05) — bất biến trong đơn |
| SagaStep | VO (enum) | `RESERVE_STOCK · PROCESS_PAYMENT · NOTIFY · DONE` |
| SagaStatus | VO (enum) | `IN_PROGRESS · COMPLETED · FAILED` |

```
CartAggregate (AR: Cart)
 └── CartItem (E, 0..*)

OrderAggregate (AR: Order)
 ├── OrderItem       (E, 1..*)
 └── OrderStatusLog  (E, 0..*)
 VO: OrderStatus, PaymentMethod, Money, ShippingAddress

SagaStateAggregate (AR: SagaState)
 VO: SagaStep, SagaStatus
```

**Invariant chính:**
- `Order.total = subtotal − discount`; chỉ chuyển trạng thái hợp lệ (PENDING→CONFIRMED/CANCELLED…) qua method `confirm()/cancel()`.
- `ShippingAddress` là snapshot bất biến — không đổi khi user sửa địa chỉ gốc.
- `SagaState` tách AR riêng để cập nhật độc lập với Order (tránh khoá toàn aggregate khi nhận event).

**Trạng thái code hiện tại:** đã có `Order` (AR), `OrderItem` (E), `SagaState` (AR), `OrderStatus` (VO).
**Cần bổ sung:** `Cart` (AR) + `CartItem` (E), `OrderStatusLog` (E), VO `PaymentMethod`, `Money`, `ShippingAddress`, enum `SagaStep`/`SagaStatus` (hiện đang là inner enum trong `SagaState` — có thể tách ra `vo/`).

---

## 4. payment-service — Context: Payment

| Tên | Loại | Vai trò |
|---|---|---|
| **Payment** | **AR** | Giao dịch thanh toán cho 1 order |
| Money | VO | Số tiền giao dịch |
| PaymentMethod | VO (enum) | `COD · VNPAY` |
| PaymentStatus | VO (enum) | `PENDING · PAID · FAILED · REFUNDED` |
| VnpayTransaction | VO | `txnRef` + `responseCode` + `payload` (kết quả VNPay) |

```
PaymentAggregate (AR: Payment)
 VO: Money, PaymentMethod, PaymentStatus, VnpayTransaction
```

**Invariant chính:**
- 1 `Payment` ↔ 1 `orderId` (unique). Chuyển trạng thái: PENDING→PAID/FAILED, PAID→REFUNDED — qua `markPaid()/markFailed()`.

**Trạng thái code hiện tại:** đã có `Payment` (AR). **Cần bổ sung:** VO `Money`, `PaymentMethod`, `PaymentStatus`, `VnpayTransaction` (hiện `method/status` đang là `String`).

---

## 5. notification-service — Context: Notification

| Tên | Loại | Vai trò |
|---|---|---|
| **NotificationLog** | **AR** | Bản ghi 1 email đã/đang gửi |
| NotificationType | VO (enum) | `ORDER_CONFIRMED · ORDER_CANCELLED · ORDER_SHIPPED · RESET_PASSWORD` |
| NotificationStatus | VO (enum) | `PENDING · SENT · FAILED` |
| EmailAddress | VO | Địa chỉ người nhận hợp lệ |

```
NotificationLogAggregate (AR: NotificationLog)
 VO: NotificationType, NotificationStatus, EmailAddress
```

**Invariant chính:** trạng thái chỉ đi PENDING→SENT/FAILED qua `markSent()/markFailed()`.

**Trạng thái code hiện tại:** đã có `NotificationLog` (AR). **Cần bổ sung:** VO `NotificationType`, `NotificationStatus`, `EmailAddress` (hiện `type/status` đang là `String`).

---

## 6. Bảng tổng hợp toàn hệ thống

| Service | Aggregate Root | Entity (trong AR) | Value Object |
|---|---|---|---|
| user | **User**, **Voucher** | Address, RefreshToken, VoucherUsage | Role, Email, DiscountPolicy |
| product | **Product**, **Category** | — | ProductVariant, Money, ProductStatus, Sku |
| order | **Cart**, **Order**, **SagaState** | CartItem, OrderItem, OrderStatusLog | OrderStatus, PaymentMethod, Money, ShippingAddress, SagaStep, SagaStatus |
| payment | **Payment** | — | Money, PaymentMethod, PaymentStatus, VnpayTransaction |
| notification | **NotificationLog** | — | NotificationType, NotificationStatus, EmailAddress |

**Tổng:** 9 Aggregate Root · 6 Entity · ~18 Value Object.

---

## 7. Cross-context — tham chiếu bằng ID (không chia sẻ aggregate)

```
Order.userId            → User.id        (REST hoặc header X-User-Id)
Order.voucherCode       → Voucher.code   (REST /api/vouchers/validate)
OrderItem.productId/sku → Product / Variant (snapshot, không FK)
Payment.orderId         → Order.id       (qua event MQ)
NotificationLog.orderId → Order.id       (qua event MQ)
```

> Money là VO **dùng chung khái niệm** nhưng được định nghĩa **riêng trong mỗi context** (không share class qua module) để giữ tính độc lập của bounded context. Nếu muốn DRY, có thể đưa `Money` vào `shared/common-web` — đánh đổi giữa tái sử dụng và độc lập context.

---

## 8. Việc cần làm tiếp (gap so với code hiện tại)

| Service | Class cần thêm |
|---|---|
| user | `Address`, `RefreshToken`, `Voucher`, `VoucherUsage` (entity) · `Email`, `DiscountPolicy` (VO) |
| product | `Category` (AR) · `Money`, `ProductStatus`, `Sku` (VO) · method trừ kho trong `Product` |
| order | `Cart` + `CartItem`, `OrderStatusLog` · `PaymentMethod`, `Money`, `ShippingAddress` (VO) · tách `SagaStep/SagaStatus` ra `vo/` |
| payment | `Money`, `PaymentMethod`, `PaymentStatus`, `VnpayTransaction` (VO) |
| notification | `NotificationType`, `NotificationStatus`, `EmailAddress` (VO) |

---

*Cartzilla Domain Model v1.0 — DDD tactical design · 9 Aggregate · 6 Entity · 18 VO*
