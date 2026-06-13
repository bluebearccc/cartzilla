# SRS — Cartzilla Fashion E-commerce Microservices

**Version:** 2.2  
**Date:** 2026-06-08  
**Team:** 4 members (Dev1–Dev4)  
**Backend:** Java 21 + Spring Boot **3.5.14** + Spring Cloud **2025.0.0** | **Frontend:** React 18 + TypeScript + Vite  
**Sources:** [ARCHITECTURE.md](./ARCHITECTURE.md), [DBDesign_Cartzilla.md](./DBDesign_Cartzilla.md), [DomainModel_Cartzilla.md](./DomainModel_Cartzilla.md)

> **Căn chỉnh theo codebase:** Chức năng nghiệp vụ là **Cartzilla** (fashion e-commerce), kiến trúc và cấu trúc service tuân theo repo `SE1911-JV_MSS301`: Maven multi-module monorepo, Eureka + Config Server + API Gateway, DDD/Hexagonal, package `com.cartzilla.<service>`.

### Service Map (chức năng → service trong repo)

| Service (repo) | Domain Cartzilla | Database | Aggregate Roots | Port | Owner |
|---|---|---|---|---|---|
| `user-service` | Auth, profile, địa chỉ, OAuth, voucher | `cartzilla_user_db` / PostgreSQL | `UserAggregate`, `VoucherAggregate` | 8081 | Dev2 |
| `product-service` | Catalog, category, vendor, variant, image, stock | `cartzilla_product_db` / PostgreSQL | `ProductAggregate`, `CategoryAggregate`, `VendorAggregate` | 8082 | Dev3 |
| `order-service` | Cart, order, status log, Saga | `cartzilla_order_db` / PostgreSQL | `CartAggregate`, `OrderAggregate`, `OrderSagaAggregate` | 8083 | Dev4 |
| `payment-service` | COD, VNPay, payment transaction audit | `cartzilla_pay_db` / PostgreSQL | `PaymentAggregate` | 8084 | Dev1 |
| `notification-service` | In-app notification, outbound email log | `cartzilla_notif_db` / PostgreSQL | `NotificationAggregate` | 8085 | Dev1 |
| `infra/api-gateway` | Routing, JWT validation, CORS | — | — | 8080 | Dev1 |
| `infra/eureka-server` | Service registry | — | — | 8761 | Dev1 |
| `infra/config-server` | Centralized config | — | — | 8888 | Dev1 |

---

## 1. Giới thiệu

### 1.1 Mục đích

Tài liệu này mô tả yêu cầu chức năng, phi chức năng, business rules, acceptance criteria và traceability cho hệ thống **Cartzilla** — nền tảng bán hàng thời trang online theo kiến trúc microservices.

Bản v2.2 cập nhật SRS theo `DBDesign_Cartzilla.md` v2.2 và Domain Model hiện có, đặc biệt là quyết định chuyển `product-service` sang PostgreSQL 16/Flyway/JPA cùng các aggregate/entity đã chi tiết hóa: `OAuthAccount`, `Vendor`, `ProductImage`, `PaymentTransaction`, `Notification`, `EmailLog`, `VoucherUsage`, `VoucherAllowedUser`, `OrderStatusLog`, `SagaState`.

### 1.2 Phạm vi

Hệ thống hỗ trợ:

- Guest duyệt, tìm kiếm, lọc và xem chi tiết sản phẩm thời trang.
- Customer đăng ký, đăng nhập, refresh token, quản lý hồ sơ và địa chỉ.
- Customer đăng nhập bằng mật khẩu hoặc OAuth provider được hỗ trợ.
- Customer thêm sản phẩm vào giỏ, checkout COD/VNPay, áp voucher hợp lệ.
- Customer xem lịch sử đơn, chi tiết đơn, trạng thái đơn và thông báo.
- Staff xử lý đơn hàng theo state machine và ghi audit log trạng thái.
- Admin quản lý catalog: category, vendor, product, variant, product image.
- Admin quản lý voucher, tài khoản, role và dashboard báo cáo cơ bản.
- Hệ thống xử lý Saga checkout, payment, stock compensation và notification/event log.

### 1.3 Định nghĩa

| Từ viết tắt | Ý nghĩa |
|---|---|
| COD | Cash on Delivery — thanh toán khi nhận hàng |
| SKU | Stock Keeping Unit — mã biến thể sản phẩm |
| JWT | JSON Web Token |
| OAuth | Đăng nhập qua Google |
| Saga | Distributed transaction pattern |
| MQ | Message Queue, dùng RabbitMQ |
| VO | Value Object trong DDD |
| Aggregate Root | Root quản lý invariant và lifecycle của nhóm entity |
| Snapshot | Dữ liệu copy tại thời điểm nghiệp vụ, không đọc live từ service nguồn |

---

## 2. Actors, External Systems & Phân quyền

### 2.1 Actors

| Actor | Mô tả | Role trong hệ thống |
|---|---|---|
| **Guest** | Người dùng chưa đăng nhập | Không có role |
| **Customer** | Người dùng đã đăng ký và đang hoạt động | `CUSTOMER` |
| **Staff** | Nhân viên xử lý đơn hàng | `STAFF` |
| **Admin** | Quản trị viên toàn hệ thống | `ADMIN` |
| **System/Saga** | Job, consumer, publisher nội bộ | Technical actor |

### 2.2 External Systems

| System | Mục đích | Giao tiếp |
|---|---|---|
| VNPay mock/provider | Xử lý redirect, callback thanh toán VNPay | REST callback + event |
| OAuth providers | Đăng nhập Google | OAuth redirect/callback |
| Cloudinary | Lưu ảnh sản phẩm | REST API |
| RabbitMQ | Event-driven checkout/payment/notification | MQ |
| MailHog/SMTP | Gửi email dev/prod | SMTP |
| Eureka/Config Server | Discovery và centralized config | Spring Cloud |

### 2.3 Phân quyền theo resource

| Resource / Capability | Guest | Customer | Staff | Admin | System/Saga |
|---|---|---|---|---|---|
| Xem sản phẩm public | Có | Có | Có | Có | Không |
| Quản lý giỏ hàng / checkout | Không | Có | Không | Không | Không |
| Xem đơn hàng của mình | Không | Có | Không | Không | Không |
| Hủy đơn của mình ở trạng thái `PENDING` | Không | Có | Không | Không | Không |
| Xem và cập nhật đơn hàng | Không | Không | Có | Có | Không |
| Quản lý catalog/category/vendor | Không | Không | Không | Có | Không |
| Quản lý voucher | Không | Không | Không | Có | Không |
| Quản lý user/role | Không | Không | Không | Có | Không |
| Xem báo cáo | Không | Không | Không | Có | Không |
| Ghi status log/payment transaction/email log | Không | Không | Không | Không | Có |
| Publish/consume Saga events | Không | Không | Không | Không | Có |

---

## 3. Use Cases

### UC-01: Duyệt & Tìm kiếm Sản phẩm (Guest / Customer)

**Precondition:** Không yêu cầu đăng nhập.  
**Main Flow:**

1. User truy cập trang chủ và xem danh sách sản phẩm nổi bật.
2. User chọn category hoặc tìm theo keyword.
3. User lọc theo size, màu sắc, khoảng giá, vendor/brand và trạng thái còn hàng.
4. User sắp xếp theo mới nhất, giá tăng, giá giảm hoặc nổi bật.
5. User mở chi tiết sản phẩm để xem ảnh, mô tả, variant, SKU, giá và tồn kho.

**Alternative Flow A1:** Không tìm thấy sản phẩm → hiển thị trạng thái rỗng rõ ràng.  
**Alternative Flow A2:** Product/category/vendor inactive hoặc soft-deleted → không hiển thị trong public catalog.

### UC-02: Quản lý Tài khoản, OAuth, Hồ sơ và Địa chỉ (Customer)

**Main Flow — Đăng ký bằng email/password:**

1. User nhập email, mật khẩu và họ tên.
2. Hệ thống validate email unique, password policy và format dữ liệu.
3. Hệ thống tạo `User` với `role = CUSTOMER`, `emailVerified = false`, `isActive = true`.
4. Hệ thống redirect đến trang đăng nhập hoặc tự đăng nhập theo cấu hình sản phẩm.

**Main Flow — Đăng nhập bằng email/password:**

1. User nhập email và mật khẩu.
2. Hệ thống xác thực, kiểm tra `isActive = true`.
3. Hệ thống trả JWT access token 15 phút và refresh token 7 ngày.
4. Refresh token được lưu server-side để hỗ trợ revoke/multi-device.

**Main Flow — OAuth login/link account:**

1. User chọn provider Google.
2. Hệ thống nhận callback và xác thực provider user id.
3. Nếu OAuth account chưa liên kết, hệ thống tạo hoặc liên kết `OAuthAccount`.
4. Mỗi `(provider, providerUserId)` chỉ liên kết một user.

**Main Flow — Quản lý địa chỉ:**

1. Customer thêm/sửa/xóa địa chỉ giao hàng.
2. Địa chỉ đầu tiên tự động là default.
3. Khi set một address là default, các address khác của user phải unset default.

**Alternative Flow A1:** Quên mật khẩu → hệ thống gửi email reset password, link có hiệu lực 30 phút.  
**Alternative Flow A2:** User bị deactivated → từ chối login, refresh token, checkout và dùng voucher.  
**Alternative Flow A3:** Xóa address default khi còn address khác → yêu cầu chuyển default trước.

### UC-03: Giỏ hàng & Checkout (Customer)

**Precondition:** Customer đã đăng nhập, `isActive = true`.  
**Main Flow:**

1. Customer chọn variant/SKU và thêm vào cart.
2. Nếu SKU đã có trong cart, hệ thống cộng quantity thay vì tạo dòng trùng.
3. Customer xem cart, chỉnh quantity hoặc xóa item.
4. Customer checkout với ít nhất một cart item và một shipping address hợp lệ.
5. Hệ thống refresh product snapshot/price/stock trước khi tạo order.
6. Customer áp voucher nếu có.
7. Hệ thống tạo order `PENDING`, copy `ShippingAddress` snapshot, copy order item snapshot.
8. Hệ thống tạo `SagaState` và chạy flow `RESERVE_STOCK → PROCESS_PAYMENT → NOTIFY → DONE`.
9. Customer nhận kết quả checkout và notification/email liên quan.

**Alternative Flow A1:** Cart rỗng → từ chối checkout.  
**Alternative Flow A2:** Stock không đủ → checkout thất bại, không tạo order confirmed.  
**Alternative Flow A3:** Voucher không hợp lệ → trả lỗi validation, customer có thể checkout không dùng voucher.  
**Alternative Flow A4:** Payment thất bại → order `CANCELLED`, stock được release, ghi payment transaction và notification hủy.

### UC-04: Quản lý Đơn hàng — Staff/Admin

**Precondition:** Đăng nhập với role `STAFF` hoặc `ADMIN`.  
**Main Flow:**

1. Staff xem danh sách đơn hàng, lọc theo trạng thái, ngày tạo, payment method.
2. Staff mở chi tiết đơn để xem item snapshot, shipping snapshot, payment status và status history.
3. Staff cập nhật trạng thái theo state machine:

```text
PENDING -> CONFIRMED -> SHIPPING -> DELIVERED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
```

4. Mỗi lần chuyển trạng thái phải ghi `OrderStatusLog`.
5. Khi COD chuyển `SHIPPING -> DELIVERED`, payment status chuyển `PAID`.

**Alternative Flow A1:** Chuyển trạng thái không hợp lệ → từ chối và không ghi log.  
**Alternative Flow A2:** Hủy đơn không có `cancelledReason` → từ chối.  
**Alternative Flow A3:** `DELIVERED` hoặc `CANCELLED` là terminal state, không chuyển tiếp.

### UC-05: Quản lý Catalog, Category, Vendor, Variant, Image — Admin

**Precondition:** Đăng nhập với role `ADMIN`.  
**Main Flow:**

1. Admin tạo/sửa/deactivate category theo cây cha-con.
2. Admin tạo/sửa/deactivate vendor với loại `SUPPLIER`, `BRAND` hoặc `MANUFACTURER`.
3. Admin tạo product với category active, vendor active nếu có.
4. Admin thêm variant gồm SKU, size, color, color hex, price, stock.
5. Admin thêm product images, đánh dấu một ảnh primary duy nhất.
6. Admin soft-delete product/category/vendor theo business rules.

**Alternative Flow A1:** Slug/SKU trùng → từ chối lưu.  
**Alternative Flow A2:** Product sellable không có variant active hoặc image → không được publish/sellable.  
**Alternative Flow A3:** Deactivate category đang có product active → từ chối.

### UC-06: Quản lý Voucher — Admin / Customer

**Admin Main Flow:**

1. Admin tạo voucher với code, discount type, discount value, min/max discount amount, usage limit, hiệu lực thời gian và nhóm user được dùng.
2. Hệ thống normalize code uppercase và đảm bảo unique.
3. Admin cấu hình audience: `ALL_USERS`, `NEW_CUSTOMER`, `LOYAL_CUSTOMER` hoặc `SPECIFIC_USERS`.
4. Admin xem usage count và trạng thái voucher.

**Customer Main Flow:**

1. Customer nhập voucher khi checkout.
2. Hệ thống validate voucher và eligibility của user để trả discount preview; bước validate không tăng `usedCount`.
3. Hệ thống tạo order với `voucherCode` và discount snapshot.
4. Khi checkout/payment thành công, hệ thống redeem voucher, ghi `VoucherUsage` idempotent theo `(voucherId, orderId)` và tăng `usedCount` atomically.

**Alternative Flow A1:** `accountAgeDays < minAccountAgeDays` → trả HTTP 422.  
**Alternative Flow A2:** Voucher đã dùng hết hoặc expired → từ chối.  
**Alternative Flow A3:** Redeem lại cùng order → trả kết quả idempotent, không tăng `usedCount` lần hai.

**Alternative Flow A4:** User nhập voucher nhưng user-service lỗi → checkout trả lỗi validate voucher, không âm thầm bỏ discount.

### UC-07: Thanh toán COD/VNPay và Payment Audit

**Main Flow — COD:**

1. Hệ thống tạo `Payment` với method `COD`, status `PENDING`.
2. Khi order `DELIVERED`, payment status chuyển `PAID`.
3. Hệ thống ghi `PaymentTransaction` append-only cho init/pay event.

**Main Flow — VNPay:**

1. Customer chọn VNPay và được redirect đến payment provider/mock.
2. Provider gọi callback về hệ thống.
3. Hệ thống verify callback, chống callback trùng theo provider transaction reference.
4. Callback success → payment `PAID`, order `CONFIRMED`.
5. Callback failed → payment `FAILED`, order `CANCELLED`, stock release.

**Alternative Flow A1:** Payment amount không khớp order total → từ chối và ghi transaction failed.  
**Alternative Flow A2:** Refund → ghi `PaymentTransaction` type `REFUND`, payment status `REFUNDED`.

### UC-08: Notification & Email Log

**Main Flow:**

1. Notification-service consume event order confirmed/cancelled/shipped hoặc reset password.
2. Hệ thống tạo `Notification` in-app nếu có recipient.
3. Hệ thống tạo `EmailLog` outbound nếu cần gửi email.
4. Khi gửi thành công, `EmailLog.status = SENT` và set `sentAt`.
5. Customer đọc notification → status chuyển `READ`, set `readAt`.

**Alternative Flow A1:** Email gửi thất bại → `EmailLog.status = FAILED`, lưu error.  
**Alternative Flow A2:** Reset password email có thể chỉ tạo `EmailLog`, không cần `Notification`.

### UC-09: Báo cáo Admin

**Precondition:** Đăng nhập với role `ADMIN`.  
**Main Flow:**

1. Admin xem tổng doanh thu, số đơn theo trạng thái, payment status và top sản phẩm bán chạy.
2. Hệ thống tổng hợp read-only từ order/payment/product data.
3. Báo cáo có filter theo khoảng ngày.

---

## 4. Yêu cầu chức năng

### 4.1 Must Have

| ID | Tên | Actor | UC | Mô tả |
|---|---|---|---|---|
| **F01** | Authentication & token | Guest/Customer | UC-02 | Đăng ký, đăng nhập, đăng xuất, refresh token, revoke token khi đổi email/mật khẩu |
| **F02** | Profile & address | Customer | UC-02 | Cập nhật hồ sơ, quản lý address, chỉ một default address |
| **F03** | Product browse/search/filter | Guest/Customer | UC-01 | Xem catalog public, tìm kiếm, lọc, sort, phân trang |
| **F04** | Product detail | Guest/Customer | UC-01 | Xem ảnh, category, vendor, variant, SKU, giá và tồn kho |
| **F05** | Cart management | Customer | UC-03 | Thêm/sửa/xóa item, unique theo `(userId, sku)`, snapshot product data |
| **F06** | Checkout & order creation | Customer | UC-03 | Validate cart/address/stock/voucher, tạo order `PENDING`, snapshot address/item |
| **F07** | Checkout Saga | System/Saga | UC-03 | Tạo `SagaState`, reserve stock, process payment, notify, compensation khi lỗi |
| **F08** | COD payment | Customer/System | UC-07 | Tạo payment COD, chuyển `PAID` khi order delivered |
| **F09** | Customer orders | Customer | UC-03 | Xem danh sách/chi tiết đơn của mình và trạng thái hiện tại |
| **F10** | Staff order workflow | Staff/Admin | UC-04 | Xem, lọc, chuyển trạng thái đơn, yêu cầu cancel reason, ghi status log |
| **F11** | Admin catalog | Admin | UC-05 | CRUD/deactivate category, product, variant, image; enforce sellable rules |
| **F12** | Notification/email | System/Customer | UC-08 | Tạo in-app notification và email log từ event/reset password |

### 4.2 Should Have

| ID | Tên | Actor | UC | Mô tả |
|---|---|---|---|---|
| **F13** | VNPay payment | Customer/System | UC-07 | Redirect, callback, verify result, idempotent callback, refund tracking |
| **F14** | Voucher | Admin/Customer | UC-06 | CRUD voucher, audience, validate/redeem, `VoucherUsage`, min account age |
| **F15** | OAuth login/link | Guest/Customer | UC-02 | Login/link Google qua `OAuthAccount` |
| **F16** | Vendor management | Admin | UC-05 | CRUD/deactivate vendor, vendor type, link product to active vendor |
| **F17** | Admin user/role management | Admin | UC-02 | Quản lý trạng thái user và role `CUSTOMER`/`STAFF`/`ADMIN` |
| **F18** | Admin reports | Admin | UC-09 | Revenue, order status, payment status, top products |

### 4.3 Out of Scope cho MVP

| ID | Nội dung |
|---|---|
| **OOS-01** | Multi-currency ngoài `VND` |
| **OOS-02** | Marketplace multi-seller đầy đủ, commission, settlement |
| **OOS-03** | Real-time chat/support |
| **OOS-04** | Recommendation engine/AI personalization |
| **OOS-05** | Inventory warehouse nâng cao, batch/lot/serial |

---

## 5. Business Rules & Validation Rules

### 5.1 Cross-cutting Rules

| ID | Mô tả |
|---|---|
| **BR-G01** | `createdAt` được set một lần khi tạo và không được sửa sau đó. |
| **BR-G02** | `updatedAt` luôn lớn hơn hoặc bằng `createdAt`. |
| **BR-G03** | Khi soft delete, `isDeleted = true`, `deletedAt` phải được set và không trước `createdAt`. |
| **BR-G04** | Không JOIN/FK cross-database; liên kết liên service chỉ lưu UUID và snapshot tại thời điểm nghiệp vụ. |
| **BR-G05** | `Money` luôn ≥ 0, scale 2 chữ số thập phân, currency mặc định `VND`. |
| **BR-G06** | Timestamp nghiệp vụ như `confirmedAt`, `paidAt`, `expiresAt`, `readAt`, `sentAt` không được trước `createdAt` của cùng record. |

### 5.2 User, Auth, OAuth, Address

| ID | Mô tả |
|---|---|
| **BR-U01** | Mỗi user phải có ít nhất một cách đăng nhập: `passwordHash` hoặc ít nhất một `OAuthAccount`. |
| **BR-U02** | Email unique trong user-service; lookup email nên normalize lowercase. |
| **BR-U03** | User mới mặc định `role = CUSTOMER`, `emailVerified = false`, `isActive = true`. |
| **BR-U04** | Chỉ user `isActive = true` mới được checkout, dùng voucher hoặc nhận refresh token mới. |
| **BR-U05** | Đổi email reset `emailVerified = false` và revoke refresh tokens. |
| **BR-U06** | Một user chỉ có một default address; address đầu tiên tự động là default. |
| **BR-U07** | Customer phải có ít nhất một address hợp lệ trước khi checkout. |
| **BR-U08** | Không hard-delete user đã có lịch sử order; chỉ soft delete sau khi cross-service check. |
| **BR-U09** | `ADMIN` mới được gán role `STAFF`/`ADMIN`; không được tự hạ role nếu còn là admin duy nhất. |
| **BR-U10** | OAuth provider chỉ nhận `GOOGLE`; unique theo `(provider, providerUserId)` và `(userId, provider)`. |

### 5.3 Product, Category, Vendor, Stock

| ID | Mô tả |
|---|---|
| **BR-P01** | Product public/sellable phải `active = true`, category active, có ít nhất một active variant và ít nhất một image. |
| **BR-P02** | `slug` category/product/vendor unique; nếu không nhập thì auto-generate từ name. |
| **BR-P03** | SKU unique toàn hệ thống, uppercase alphanumeric + dấu `-`, max 50 ký tự. |
| **BR-P04** | `stock` của mọi variant luôn ≥ 0; từ chối reserve nếu stock không đủ. |
| **BR-P05** | Mỗi product chỉ có một primary image. |
| **BR-P06** | Order item lưu snapshot `productId`, `sku`, `name`, `image`, `size`, `color`, `price`; thay đổi catalog sau đó không làm đổi order cũ. |
| **BR-P07** | Không deactivate category nếu còn product active thuộc category đó. |
| **BR-P08** | Vendor inactive không được gán cho product mới; product cũ vẫn giữ reference snapshot/id. |

### 5.4 Cart, Order, Saga

| ID | Mô tả |
|---|---|
| **BR-O01** | Cart unique theo `(userId, sku)`; thêm lại cùng SKU thì cộng quantity. |
| **BR-O02** | Cart item quantity > 0; update quantity = 0 thì xóa dòng cart. |
| **BR-O03** | Không tạo order rỗng; order phải có ít nhất một `OrderItem`. |
| **BR-O04** | `shippingAddress` là JSON/snapshot bắt buộc, copy từ address tại checkout và không sync ngược. |
| **BR-O05** | `subtotal = sum(orderItem.subtotal)`, `totalAmount = subtotal - discount`, `0 <= discount <= subtotal`. |
| **BR-O06** | Sau khi order `CONFIRMED`, item, giá và địa chỉ immutable; chỉ được chuyển trạng thái/hủy theo rule. |
| **BR-O07** | `cancelledReason` bắt buộc khi order chuyển sang `CANCELLED`. |
| **BR-O08** | Mỗi chuyển trạng thái order phải ghi `OrderStatusLog` append-only. |
| **BR-O09** | `PENDING -> CONFIRMED` khi payment OK hoặc COD accepted; set `confirmedAt`. |
| **BR-O10** | `CONFIRMED -> SHIPPING` và `SHIPPING -> DELIVERED` chỉ Staff/Admin. |
| **BR-O11** | `DELIVERED` và `CANCELLED` là terminal state. |
| **BR-O12** | Mỗi order có đúng một `SagaState`; step flow không được nhảy bước: `RESERVE_STOCK -> PROCESS_PAYMENT -> NOTIFY -> DONE`. |
| **BR-O13** | Saga vượt ngưỡng retry phải chuyển `FAILED`, ghi `errorMessage` và chạy compensation nếu cần. |

### 5.5 Voucher

| ID | Mô tả |
|---|---|
| **BR-V01** | Voucher code unique, lookup không phân biệt hoa thường, normalize uppercase. |
| **BR-V02** | Voucher redeem khi active, chưa expired, `usedCount < maxUses`, đạt min order amount và đạt min account age. |
| **BR-V03** | `PERCENTAGE`: `0 < discountValue <= 100`; `FIXED_AMOUNT`: `discountValue > 0`. |
| **BR-V04** | `usedCount <= maxUses`; tăng `usedCount` atomically khi redeem thành công. |
| **BR-V05** | Mỗi cặp `(voucherId, orderId)` chỉ có một `VoucherUsage`; redeem phải idempotent. |
| **BR-V06** | Không sửa voucher `code` sau khi đã có `VoucherUsage`. |
| **BR-V07** | `minAccountAgeDays >= 0`; nếu customer chưa đủ tuổi tài khoản thì trả lỗi validation HTTP 422. |
| **BR-V08** | Voucher percentage phải có `maxDiscountAmount` để giới hạn số tiền giảm tối đa. |
| **BR-V09** | User dùng voucher không được vượt `perUserLimit`. |
| **BR-V10** | Voucher chỉ được redeem sau khi checkout/payment thành công; validate chỉ preview discount và không tăng `usedCount`. |
| **BR-V11** | Với VNPay, payment success mới redeem voucher; payment failed không làm mất lượt voucher. |
| **BR-V12** | User nhập voucher nhưng validate service lỗi thì checkout phải báo lỗi, không được âm thầm bỏ voucher. |
| **BR-V13** | Discount và voucher code phải được snapshot vào order tại thời điểm checkout. |
| **BR-V14** | `audienceType` quyết định user nào được dùng voucher: `ALL_USERS`, `NEW_CUSTOMER`, `LOYAL_CUSTOMER`, `SPECIFIC_USERS`. |

### 5.6 Payment

| ID | Mô tả |
|---|---|
| **BR-PY01** | Mỗi order có đúng một `Payment`; `orderId` unique trong payment-service. |
| **BR-PY02** | Payment amount phải khớp `order.totalAmount`, tolerance 0, currency `VND`. |
| **BR-PY03** | Payment method chỉ gồm `COD` hoặc `VNPAY`. |
| **BR-PY04** | Payment status gồm `PENDING`, `PAID`, `FAILED`, `REFUNDED`. |
| **BR-PY05** | Mọi attempt/callback/refund ghi thêm `PaymentTransaction`, append-only. |
| **BR-PY06** | VNPay callback chống trùng bằng unique `(provider, providerTxnRef)` khi `providerTxnRef` khác null. |
| **BR-PY07** | COD chuyển `PAID` khi order `DELIVERED`; VNPay success callback chuyển `PAID`, failed callback chuyển `FAILED`. |

### 5.7 Notification

| ID | Mô tả |
|---|---|
| **BR-N01** | `Notification` phục vụ in-app UI; `EmailLog` phục vụ outbound email, hai lớp tách biệt. |
| **BR-N02** | Order events `order.confirmed`, `order.cancelled`, `order.shipped` có thể tạo một notification và tối đa một email log liên kết. |
| **BR-N03** | Reset password email có thể chỉ có `EmailLog`, không cần `Notification`. |
| **BR-N04** | Khi notification chuyển `READ`, set `readAt` và `readAt >= createdAt`. |
| **BR-N05** | Email status gồm `PENDING`, `SENT`, `FAILED`; khi failed phải lưu error. |

---

## 6. Yêu cầu phi chức năng

| Category | Requirement |
|---|---|
| **Performance** | API response p95 < 500ms với tải bình thường; search/list product có phân trang. |
| **Availability** | Mỗi service có health check qua `/actuator/health`. |
| **Security** | HTTPS ở môi trường triển khai; dev có thể HTTP; password hash bằng bcrypt; không log secret/token/plain password. |
| **Authorization** | Gateway và service endpoint enforce JWT role theo permission matrix. |
| **Data Consistency** | Không FK cross-database; dùng event, idempotency, snapshot và compensation. |
| **Scalability** | Mỗi service độc lập, có thể scale riêng; RabbitMQ tách xử lý bất đồng bộ. |
| **Observability** | Structured logging, correlation id, Actuator, Micrometer Prometheus/Grafana. |
| **Resilience** | Circuit Breaker/Retry/Timeout cho inter-service calls; consumer idempotent. |
| **API Docs** | Swagger UI `/swagger-ui.html` cho mỗi service có REST API. |
| **Testing** | Unit test coverage mục tiêu ≥ 60% mỗi service; có integration test cho Saga/payment critical flow. |
| **Auditability** | `OrderStatusLog`, `PaymentTransaction`, `EmailLog` append-only; entity kế thừa audit fields từ `BaseEntity`. |

---

## 7. Giao diện người dùng — Danh sách màn hình

| Route | Page | Actor | Dev | Feature |
|---|---|---|---|---|
| `/` | HomePage | Guest/Customer | Dev3 | F03 |
| `/products` | ProductListPage | Guest/Customer | Dev3 | F03 |
| `/products/:id` | ProductDetailPage | Guest/Customer | Dev3 | F04 |
| `/login` | LoginPage | Guest | Dev2 | F01 |
| `/register` | RegisterPage | Guest | Dev2 | F01 |
| `/oauth/callback` | OAuthCallbackPage | Guest/Customer | Dev2 | F15 |
| `/forgot-password` | ForgotPasswordPage | Guest | Dev2 | UC-02 A1, F12 |
| `/profile` | ProfilePage | Customer | Dev2 | F02 |
| `/addresses` | AddressPage | Customer | Dev2 | F02 |
| `/notifications` | NotificationPage | Customer | Dev1 | F12 |
| `/cart` | CartPage | Customer | Dev4 | F05 |
| `/checkout` | CheckoutPage | Customer | Dev4 | F06, F14 |
| `/checkout/payment` | PaymentPage | Customer | Dev1 | F08, F13 |
| `/payment/result` | PaymentResultPage | Customer | Dev1 | F08, F13 |
| `/orders` | OrderListPage | Customer | Dev4 | F09 |
| `/orders/:id` | OrderDetailPage | Customer | Dev4 | F09 |
| `/staff/orders` | StaffOrderListPage | Staff/Admin | Dev4 | F10 |
| `/staff/orders/:id` | StaffOrderDetailPage | Staff/Admin | Dev4 | F10 |
| `/admin/products` | AdminProductPage | Admin | Dev3 | F11 |
| `/admin/products/new` | AdminProductForm | Admin | Dev3 | F11 |
| `/admin/categories` | AdminCategoryPage | Admin | Dev3 | F11 |
| `/admin/vendors` | AdminVendorPage | Admin | Dev3 | F16 |
| `/admin/vouchers` | AdminVoucherPage | Admin | Dev2 | F14 |
| `/admin/users` | AdminUserPage | Admin | Dev2 | F17 |
| `/admin/reports` | AdminReportsPage | Admin | Dev1 | F18 |

---

## 8. Acceptance Criteria

### 8.1 Pass/Fail Test Candidates

| Test Case | Steps | Expected |
|---|---|---|
| TC-01 Register | POST `/api/users/register` với email mới và password hợp lệ | 201, user role `CUSTOMER`, `isActive = true` |
| TC-02 Duplicate email | POST `/api/users/register` với email đã tồn tại | 409 hoặc 422, không tạo user mới |
| TC-03 Login | POST `/api/users/login` đúng email/pass | 200, trả access token + refresh token |
| TC-04 Inactive login | Login bằng user `isActive = false` | 403, không phát token |
| TC-05 OAuth callback | Callback Google hợp lệ | Tạo/link `OAuthAccount`, unique provider account |
| TC-06 Default address | Set address B default khi address A đang default | B default, A không còn default |
| TC-07 Browse products | GET `/api/products?category=ao&size=M` | 200, chỉ trả product active/sellable đúng filter |
| TC-08 Product detail | GET `/api/products/{id}` product active | 200, có variant, image, vendor/category data |
| TC-09 Add duplicate SKU | POST cart cùng SKU hai lần | Cart chỉ có một dòng SKU, quantity được cộng |
| TC-10 Checkout empty cart | POST checkout khi cart rỗng | 422, không tạo order |
| TC-11 Checkout no address | POST checkout khi user chưa có address hợp lệ | 422, không tạo order |
| TC-12 Checkout COD | POST `/api/orders/checkout` với method `COD` | 201, order `PENDING`, saga started, payment `PENDING` |
| TC-13 Stock insufficient | Checkout SKU không đủ stock | Checkout fail, stock không âm, order không confirmed |
| TC-14 Voucher min age | Apply voucher yêu cầu 30 ngày cho user 5 ngày tuổi | 422, message nêu min account age |
| TC-15 Voucher idempotent | Redeem lại cùng `(voucherId, orderId)` | Không tăng `usedCount` lần hai |
| TC-16 Voucher per-user limit | User đã redeem voucher đủ `perUserLimit` | 422, không tạo usage mới |
| TC-17 Voucher audience | User không nằm trong `SPECIFIC_USERS` apply voucher | 422, reasonCode audience mismatch |
| TC-18 Voucher max discount | Apply voucher 20% max 150k cho order 1M | Discount = 150k |
| TC-19 VNPay voucher failed | VNPay payment failed sau khi preview voucher | Không tạo `VoucherUsage`, không tăng `usedCount` |
| TC-20 VNPay success | VNPay callback success hợp lệ | Payment `PAID`, order `CONFIRMED`, transaction logged |
| TC-21 VNPay duplicate callback | Gửi lại cùng providerTxnRef | Idempotent response, không ghi transaction trùng |
| TC-22 Payment amount mismatch | Callback/payment event amount khác order total | Payment failed/rejected, transaction failed logged |
| TC-23 Staff transition | PUT order `CONFIRMED -> SHIPPING` bởi Staff | 200, status đổi, `OrderStatusLog` được ghi |
| TC-24 Invalid transition | PUT order `DELIVERED -> CANCELLED` | 422, không đổi status |
| TC-25 Cancel reason required | Cancel order không có reason | 422, không đổi status |
| TC-26 COD delivered | Staff chuyển COD order `SHIPPING -> DELIVERED` | Order `DELIVERED`, payment `PAID` |
| TC-27 Notification on confirmed | Order confirmed event | Tạo `Notification` và `EmailLog` phù hợp |
| TC-28 Email failure | SMTP lỗi khi gửi email | `EmailLog.status = FAILED`, lưu error |

### 8.2 End-to-End Flow

```text
[Guest] Browse products (UC-01)
  -> Register/Login/OAuth (UC-02, F01/F15)
  -> Manage address (F02)
  -> Add to cart (F05)
  -> Checkout COD/VNPay + voucher optional (F06/F08/F13/F14)
  -> Saga reserve stock/process payment/notify (F07)
  -> Customer receives notification/email (F12)
  -> Staff confirms/ships/delivers order (F10)
  -> Customer views updated order status (F09)
```

---

## 9. Traceability Matrix (Feature → UC → Service → Endpoint → Data → Event)

| Feature | UC | Service | REST Endpoint (qua Gateway) | DB table/collection / Entity | Event liên quan |
|---|---|---|---|---|---|
| F01 Auth | UC-02 | user-service | `POST /api/users/register`, `/login`, `/refresh-token`, `/logout` | `users`, `refresh_tokens` | — |
| F02 Profile/address | UC-02 | user-service | `GET/PUT /api/users/me`, `GET/POST/PUT/DELETE /api/users/me/addresses` | `users`, `addresses` | — |
| F03 Browse/search | UC-01 | product-service | `GET /api/products?category=&size=&color=&vendor=&sort=&page=` | `products`, `categories`, `vendors` | — |
| F04 Product detail | UC-01 | product-service | `GET /api/products/{id}` | `products`, `product_variants`, `product_images` | — |
| F05 Cart | UC-03 | order-service | `GET/POST/PUT/DELETE /api/orders/cart/items` | `cart_items` | — |
| F06 Checkout/order | UC-03 | order-service | `POST /api/orders/checkout` | `orders`, `order_items`, `saga_states` | `stock.reserve`, `payment.process` |
| F07 Saga | UC-03 | order-service | Consumer/internal | `saga_states` | `stock.reserved`, `payment.result`, `stock.release`, `notification.send` |
| F08 COD payment | UC-07 | payment-service | Consumer/internal | `payments`, `payment_transactions` | `payment.process`, `payment.result`, `order.delivered` |
| F09 My orders | UC-03 | order-service | `GET /api/orders`, `GET /api/orders/{id}` | `orders`, `order_items`, `order_status_logs` | — |
| F10 Staff orders | UC-04 | order-service | `GET /api/staff/orders`, `PUT /api/staff/orders/{id}/status` | `orders`, `order_status_logs` | `order.confirmed`, `order.shipped`, `order.cancelled`, `order.delivered` |
| F11 Admin catalog | UC-05 | product-service | `POST/PUT/DELETE /api/admin/products`, `/categories`, `/products/{id}/variants`, `/products/{id}/images` | `products`, `categories`, `product_variants`, `product_images` | — |
| F12 Notification/email | UC-08 | notification-service | `GET /api/notifications`, `PUT /api/notifications/{id}/read` | `notifications`, `email_logs` | `order.confirmed`, `order.cancelled`, `order.shipped`, `reset-password` |
| F13 VNPay | UC-07 | payment-service | `POST /api/payments/vnpay/create`, `GET /api/payments/vnpay/callback` | `payments`, `payment_transactions` | `payment.result` |
| F14 Voucher | UC-06 | user-service | `POST /api/admin/vouchers`, `PUT /api/admin/vouchers/{id}`, `POST /api/vouchers/validate`, internal `POST /api/internal/vouchers/redeem` | `vouchers`, `voucher_usages`, `voucher_allowed_users` | — |
| F15 OAuth | UC-02 | user-service | `GET /api/oauth/{provider}/authorize`, `GET /api/oauth/{provider}/callback` | `oauth_accounts`, `users` | — |
| F16 Vendor | UC-05 | product-service | `GET/POST/PUT/DELETE /api/admin/vendors` | `vendors`, `products.vendorId` | — |
| F17 User/role admin | UC-02 | user-service | `GET/PUT /api/admin/users/{id}`, `/role`, `/status` | `users` | — |
| F18 Reports | UC-09 | order/product/payment-service | `GET /api/admin/reports/*` | read-only aggregate | — |

---

## 10. NFR — Ánh xạ vào kiến trúc repo

| NFR / Concern | Hiện thực trong codebase |
|---|---|
| Service Discovery | Eureka (`infra/eureka-server`), service register bằng `spring.application.name`. |
| Centralized Config | Config Server (`infra/config-server`), service import `optional:configserver:...`. |
| Gateway Security | API Gateway route theo `/api/**`, JWT validation, CORS config. |
| Audit trail | `BaseEntity`: `createdAt/updatedAt/createdBy/updatedBy/isDeleted/deletedAt`. |
| Persistence | Tất cả domain service dùng PostgreSQL 16, database-per-service và Flyway `V*.sql`; `product-service` cũng dùng PostgreSQL theo `DBDesign_Cartzilla.md` v2.2. |
| Soft delete | Tất cả bảng domain dùng `BaseEntity` audit columns và filter `is_deleted = false`; soft delete set `deleted_at`. |
| No cross-DB FK | Liên service lưu UUID/snapshot, validate qua API/event khi cần. |
| Saga consistency | RabbitMQ events: `stock.reserve`, `stock.reserved`, `stock.release`, `payment.process`, `payment.result`, notification events. |
| Idempotency | Voucher redeem theo `(voucherId, orderId)`, VNPay callback theo provider transaction ref, consumers theo event id/correlation id. |
| Security | `shared/common-security` JWT, bcrypt password, role-based authorization. |
| Resilience | Resilience4j `@CircuitBreaker`, timeout/retry cho call inter-service. |
| Observability | Actuator + Micrometer Prometheus, structured logs, correlation id. |
| API Docs | SpringDoc OpenAPI `/swagger-ui.html`. |
| Testing | JUnit5 + Mockito; integration tests cho checkout Saga, payment callback, voucher redeem. |

---

## 11. Domain Glossary (Ubiquitous Language — DDD)

| Term | Aggregate/Entity | Service | Mô tả |
|---|---|---|---|
| **User** | `UserAggregate` / `User` | user-service | Tài khoản đăng nhập, có role và trạng thái hoạt động |
| **Address** | `Address` | user-service | Địa chỉ giao hàng của user, chỉ một default |
| **RefreshToken** | `RefreshToken` | user-service | Token server-side hỗ trợ refresh/revoke/multi-device |
| **OAuthAccount** | `OAuthAccount` | user-service | Liên kết user với Google provider |
| **Voucher** | `VoucherAggregate` / `Voucher` | user-service | Mã giảm giá, usage limit, expiry, min account age |
| **VoucherUsage** | `VoucherUsage` | user-service | Bản ghi redeem voucher append-only/idempotent |
| **VoucherAllowedUser** | `VoucherAllowedUser` | user-service | Danh sách user được phép dùng voucher `SPECIFIC_USERS` |
| **Product** | `ProductAggregate` / `Product` | product-service | Sản phẩm thời trang, thuộc category, có variant/image |
| **Category** | `CategoryAggregate` / `Category` | product-service | Danh mục sản phẩm dạng cây |
| **Vendor** | `VendorAggregate` / `Vendor` | product-service | Supplier/brand/manufacturer liên kết product |
| **Variant** | `ProductVariant` | product-service | Biến thể size/màu, có SKU, giá và stock |
| **ProductImage** | `ProductImage` | product-service | Ảnh sản phẩm, có primary image và sort order |
| **Cart** | `CartAggregate` / `CartItem` | order-service | Giỏ hàng server-side theo user và SKU |
| **Order** | `OrderAggregate` / `Order` | order-service | Đơn hàng, item snapshot, shipping snapshot và trạng thái |
| **OrderItem** | `OrderItem` | order-service | Dòng hàng trong đơn, immutable sau khi tạo order |
| **OrderStatusLog** | `OrderStatusLog` | order-service | Audit log append-only cho mọi chuyển trạng thái đơn |
| **SagaState** | `OrderSagaAggregate` / `SagaState` | order-service | Trạng thái distributed transaction checkout |
| **Payment** | `PaymentAggregate` / `Payment` | payment-service | Giao dịch thanh toán gắn với một order |
| **PaymentTransaction** | `PaymentTransaction` | payment-service | Audit log append-only cho attempt/callback/refund |
| **Notification** | `NotificationAggregate` / `Notification` | notification-service | Thông báo in-app cho user |
| **EmailLog** | `EmailLog` | notification-service | Log gửi email outbound/reset password/order notification |

---

## 12. Data Requirements & DB Alignment

### 12.1 Database per Service

| Service | Database | Technology | Core Tables |
|---|---|---|---|
| user-service | `cartzilla_user_db` | PostgreSQL 16 | `users`, `addresses`, `refresh_tokens`, `oauth_accounts`, `vouchers`, `voucher_usages`, `voucher_allowed_users` |
| product-service | `cartzilla_product_db` | PostgreSQL 16 | `categories`, `vendors`, `products`, `product_variants`, `product_images` |
| order-service | `cartzilla_order_db` | PostgreSQL 16 | `cart_items`, `orders`, `order_items`, `order_status_logs`, `saga_states` |
| payment-service | `cartzilla_pay_db` | PostgreSQL 16 | `payments`, `payment_transactions` |
| notification-service | `cartzilla_notif_db` | PostgreSQL 16 | `notifications`, `email_logs` |

### 12.2 Required DB Constraints

| Area | Required constraints from DBDesign |
|---|---|
| BaseEntity | Mọi bảng domain có `created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted`, `deleted_at`. |
| Auth/OAuth | `users.email` unique; `refresh_tokens.token` unique; `oauth_accounts(provider, provider_user_id)` và `(user_id, provider)` unique. |
| Voucher | `upper(vouchers.code)` unique; `min_account_age_days >= 0`; `voucher_usages` lưu `user_id` và `order_id` reference-only; `voucher_allowed_users` dùng cho audience `SPECIFIC_USERS`. |
| Catalog | `categories.slug`, `vendors.slug`, `products.slug`, `product_variants.sku` unique; price/stock không âm. |
| Order | `cart_items(user_id, sku)` unique; `orders.shipping_address` là JSONB snapshot; `saga_states.order_id` unique. |
| Payment | `payments.order_id` unique; `payment_transactions(provider, provider_txn_ref)` unique khi provider ref khác null. |
| Notification | `email_logs.notification_id` là FK nội bộ notification-service; `order_id` là reference-only. |

### 12.3 Cross-Service References

| Reference | Source DB | Target | Handling |
|---|---|---|---|
| `cart_items.product_id`, `order_items.product_id` | order DB | product-service `products.id` | Lưu UUID + snapshot product data, không FK cross-DB. |
| `orders.user_id` | order DB | user-service `users.id` | Validate/query qua user-service API khi cần. |
| `orders.shipping_address` | order DB | — | JSONB snapshot, không ref live `addresses`. |
| `voucher_usages.order_id` | user DB | order-service `orders.id` | Reference-only, redeem idempotent. |
| `payments.order_id`, `payment_transactions.order_id` | payment DB | order-service `orders.id` | Đồng bộ qua MQ/API, không FK cross-DB. |
| `notifications.order_id`, `email_logs.order_id` | notification DB | order-service `orders.id` | Nhận từ event order. |

### 12.4 Migration & Seed Requirements

| ID | Requirement |
|---|---|
| **DR-01** | Dev có thể dùng `ddl-auto: update`; production phải dùng Flyway `V*.sql` và `ddl-auto: validate`. |
| **DR-02** | Product-service migration phải chuyển dữ liệu catalog từ MongoDB cũ sang PostgreSQL nếu đã có dữ liệu trước đó. |
| **DR-03** | Seed dev tối thiểu gồm admin/staff/customer, category, vendor, product + variant stock, voucher mẫu, notification/email mẫu. |
| **DR-04** | `notification_logs` cũ, nếu tồn tại, phải được tách thành `notifications` và `email_logs`. |
| **DR-05** | Payment migration phải thêm `payment_transactions`; `payments.status` chỉ là trạng thái tổng hợp mới nhất. |

---

## 13. Assumptions & Open Questions

| ID | Type | Nội dung | Impact |
|---|---|---|---|
| **A-01** | Assumption | `DBDesign_Cartzilla.md` v2.2 là nguồn authoritative cho persistence; nếu `ARCHITECTURE.md` còn dòng cũ nói product-service dùng MongoDB thì cần sync lại architecture sau. | Backend/API/DevOps đi theo PostgreSQL 16 + Flyway cho product-service. |
| **A-02** | Assumption | VNPay trong MVP có thể dùng mock provider; production VNPay config nằm ngoài SRS này. | DevOps cần bổ sung env vars và secret handling. |
| **Q-01** | Open Question | OAuth login là Should Have hay cần đưa vào Must Have cho demo? | Ảnh hưởng scope Dev2 và UI auth flow. |
| **Q-02** | Open Question | Admin reports lấy dữ liệu live từ services hay dùng read model riêng? | Ảnh hưởng hiệu năng và thiết kế API/reporting. |

---

*Cartzilla SRS v2.2 — Microservices E-commerce · Java 21 + Spring Boot 3.5.14 + Spring Cloud 2025.0.0 · DDD/Hexagonal · aligned with DBDesign_Cartzilla.md v2.2 and DomainModel on 2026-06-08*
