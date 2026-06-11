# Cartzilla — Phase 1: Fix OpenFeign & Core Flow

## 1.1 Fix notification email bug
> `OrderSagaOrchestrator` publish event với `recipientEmail = null` → email luôn gửi đến `customer@cartzilla.local`

- [ ] Inject `UserFeignClient` vào `OrderSagaOrchestrator`
- [ ] Trong `onPaymentResult()` — gọi `getUserById(order.getUserId())` lấy email trước khi publish
- [ ] Truyền `userDto.email()` vào `OrderConfirmedEvent(orderId, email)` thay vì `null`
- [ ] Trong `fail()` — tương tự, truyền email vào `OrderCancelledEvent(orderId, reason, email)`
- [ ] Test thủ công: checkout → kiểm tra MailHog nhận đúng email

---

## 1.2 Wire voucher validation vào checkout
> `CheckoutUseCase` hardcode `discount = BigDecimal.ZERO`, bỏ qua `cmd.voucherCode()`

- [ ] Inject `UserFeignClient` vào `CheckoutUseCase`
- [ ] Khi `cmd.voucherCode() != null && !cmd.voucherCode().isBlank()`:
  - [ ] Gọi `validateVoucher(code, userId, subtotal)`
  - [ ] Lấy `discountAmount` từ response gán vào `discount`
- [ ] Khi voucher invalid (Feign throw `BusinessException`) — propagate lỗi lên caller
- [ ] Khi không có voucher — giữ `discount = BigDecimal.ZERO` như cũ
- [ ] Test thủ công: checkout với voucher hợp lệ → kiểm tra `Order.discount` đúng giá trị

---

## 1.3 Cart CRUD
> `CartItem` entity và DB migration đã có, chưa có controller và use cases

### Controller
- [x] Tạo `CartController` mapping `/api/orders/cart`
  - [x] `POST /api/orders/cart` — thêm item (body: `{ sku, quantity }`)
  - [x] `GET /api/orders/cart` — xem giỏ hàng của user hiện tại
  - [x] `PUT /api/orders/cart/{sku}` — cập nhật số lượng (body: `{ quantity }`)
  - [x] `DELETE /api/orders/cart/{sku}` — xóa item khỏi giỏ
  - [x] `DELETE /api/orders/cart` — xóa toàn bộ giỏ

### Use Cases
- [x] `AddToCartUseCase`
  - [x] Inject `ProductFeignClient`, gọi `getVariantBySku(sku)` lấy snapshot
  - [x] Validate: variant `active = true`, `stock >= quantity` (CTA-03, X-01)
  - [x] Nếu SKU đã có trong giỏ → cộng dồn quantity
  - [x] Lưu snapshot: `productName`, `image`, `size`, `color`, `unitPrice` tại thời điểm add (CTA-05)
- [x] `GetCartUseCase` — lấy danh sách `CartItem` theo `userId`
- [x] `UpdateCartItemUseCase` — cập nhật `quantity`, validate lại stock
- [x] `RemoveCartItemUseCase` — xóa theo `sku + userId`
- [x] `ClearCartUseCase` — xóa toàn bộ cart của user (gọi sau checkout thành công)

### DTOs
- [x] `CartItemRequest` — `{ sku, quantity }`
- [x] `CartItemResponse` — `{ sku, productName, image, size, color, unitPrice, quantity, subtotal }`
- [x] `CartResponse` — `{ items: CartItemResponse[], total }`

### Tích hợp với Checkout
- [x] Sau saga hoàn tất (`onPaymentResult` success) → gọi `ClearCartUseCase` để xóa giỏ hàng

### Repository
- [x] Kiểm tra `CartItemJpaRepository` đã có các method cần thiết:
  - [x] `findByUserId(UUID userId)`
  - [x] `findByUserIdAndSku(UUID userId, String sku)`
  - [x] `deleteByUserId(UUID userId)`
  - [x] `deleteByUserIdAndSku(UUID userId, String sku)`

---

## 1.4 Fix notification-service FeignConfig thiếu ErrorDecoder
> `order-service` có `FeignErrorDecoder` xử lý 400/404/409/422, `notification-service` không có → lỗi từ `user-service` sẽ throw exception không rõ ràng

- [ ] Tạo `FeignErrorDecoder` trong `notification-service/infrastructure/feign/`
- [ ] Đăng ký `FeignErrorDecoder` vào `FeignConfig` của notification-service (thêm `@Bean ErrorDecoder`)

---

## 1.5 Block `/api/internal/**` tại API Gateway
> Các internal endpoints (`/api/internal/users`, `/api/internal/products`, `/api/internal/vouchers`) đang bị expose ra ngoài — bất kỳ ai cũng gọi được

- [ ] Thêm route filter trong `api-gateway/application.yml` để reject tất cả request đến `/api/internal/**` từ bên ngoài
- [ ] Hoặc thêm `SetPath` / `RewritePath` filter + deny rule — chặn trước khi forward đến service
- [ ] Verify: gọi trực tiếp `GET /api/internal/users/{id}` qua gateway → nhận `403` hoặc `404`
