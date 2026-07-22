
# Cartzilla — checklist test luồng xuôi và luồng ngược

Checklist này kiểm tra trực tiếp qua API Gateway, PostgreSQL và RabbitMQ. Mỗi case cần ghi PASS/FAIL, thời điểm chạy, orderId và response.

## 1. Khởi động

Chạy tại thư mục gốc:

~~~powershell
Copy-Item .env.example .env -ErrorAction SilentlyContinue
# Điền JWT_SECRET; nếu test VNPay thật thì điền VNPAY_TMN_CODE và VNPAY_HASH_SECRET.
docker compose up -d --build
docker compose ps -a
~~~

Health phải trả 200:

~~~powershell
$ports = 8080,8081,8082,8083,8084,8085
foreach ($port in $ports) {
  (Invoke-WebRequest "http://localhost:$port/actuator/health").StatusCode
}
(Invoke-WebRequest http://localhost:5173).StatusCode
~~~

Service/database bind loopback. User database dùng host port 15432 mặc định vì máy có thể đã có PostgreSQL chiếm 5432.

## 2. Tài khoản và biến dùng chung

Tài khoản local seed:

~~~text
admin@cartzilla.com / Admin@123456
~~~

Chỉ dùng tài khoản này cho local và đổi mật khẩu trước production.

~~~powershell
$login = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/users/login -ContentType application/json -Body (@{ email = 'admin@cartzilla.com'; password = 'Admin@123456' } | ConvertTo-Json)
$TOKEN = $login.data.accessToken
$headers = @{ Authorization = "Bearer $TOKEN" }
$me = Invoke-RestMethod -Method GET -Uri http://localhost:8080/api/users/me -Headers $headers
$USER_ID = $me.data.id
$address = (@{ fullName = 'Cartzilla Test'; phone = '0900000000'; street = '1 Test Street'; district = 'District 1'; city = 'Ho Chi Minh City' } | ConvertTo-Json -Compress)
$SKU = 'TSN-001-M-WHT'
~~~

SKU seed: TSN-001-M-WHT.

## 3. Snapshot dữ liệu

~~~powershell
docker exec cartzilla-postgres-product-1 psql -U app -d cartzilla_product_db -c "select sku, stock from product_variants where sku = 'TSN-001-M-WHT';"
docker exec cartzilla-postgres-product-1 psql -U app -d cartzilla_product_db -c "select order_id, status from stock_reservations order by created_at desc limit 10;"
docker exec cartzilla-postgres-order-1 psql -U app -d cartzilla_order_db -c "select id, status, payment_method, payment_status, total_amount from orders order by created_at desc limit 10;"
docker exec cartzilla-postgres-pay-1 psql -U app -d cartzilla_pay_db -c "select order_id, status, vnpay_txn_ref from payments order by created_at desc limit 10;"
~~~

Invariant: order thành công chỉ trừ stock một lần; order hủy/Saga fail phải trả stock đã reserve; retry không tạo transaction hoặc refund trùng.

## 4. Luồng xuôi

### F01 — Catalog và snapshot SKU

~~~powershell
Invoke-RestMethod 'http://localhost:8080/api/products?keyword=Basic&limit=12'
Invoke-RestMethod 'http://localhost:8082/api/internal/products/variants/TSN-001-M-WHT'
~~~

Kỳ vọng: HTTP 200, SKU active, price dương, stock không âm.

### F02 — Checkout COD

~~~powershell
$checkout = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/orders/checkout/by-sku -Headers $headers -ContentType application/json -Body (@{ userId = $USER_ID; lines = @(@{ sku = $SKU; quantity = 1 }); shippingAddress = $address; paymentMethod = 'COD' } | ConvertTo-Json -Depth 10)
$ORDER_ID = $checkout.data.orderId
Start-Sleep 5
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$ORDER_ID/status" -Headers $headers
~~~

Kỳ vọng: order PENDING → CONFIRMED, Saga COMPLETED, COD payment vẫn PENDING, stock giảm đúng một.

### F03 — COD delivered

~~~powershell
Invoke-RestMethod -Method PUT -Uri "http://localhost:8080/api/staff/orders/$ORDER_ID/status" -Headers $headers -ContentType application/json -Body (@{ status = 'SHIPPING' } | ConvertTo-Json)
Invoke-RestMethod -Method PUT -Uri "http://localhost:8080/api/staff/orders/$ORDER_ID/status" -Headers $headers -ContentType application/json -Body (@{ status = 'DELIVERED' } | ConvertTo-Json)
~~~

Kỳ vọng: order DELIVERED, COD payment PENDING → PAID. Chuyển lại status hoặc giao từ PENDING phải bị từ chối.

### F04 — Voucher redeem

Tạo voucher bằng API admin, checkout có voucherCode.

Kỳ vọng:

- Checkout chỉ preview, chưa tăng used_count.
- Chỉ sau payment success mới tạo voucher_usages.
- orders.discount và orders.total_amount đúng.
- Khi hủy/compensate, voucher_usages.released=true và used_count giảm một lần.

~~~powershell
docker exec cartzilla-postgres-user-1 psql -U app -d cartzilla_user_db -c "select voucher_id, order_id, released, discount_amount from voucher_usages order by created_at desc limit 10;"
~~~

### F05 — Tạo URL VNPay

Cần điền VNPAY_TMN_CODE và VNPAY_HASH_SECRET trong .env.

~~~powershell
$payment = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/payments/vnpay/create -Headers $headers -ContentType application/json -Body (@{ orderId = $ORDER_ID; amount = 1; orderInfo = "Cartzilla test $ORDER_ID" } | ConvertTo-Json)
$payment.data.paymentUrl
~~~

Kỳ vọng: amount thật lấy từ order-service; URL có vnp_SecureHash, vnp_TxnRef và vnp_ReturnUrl.

### F06 — VNPay success callback/IPN

Thanh toán URL trên sandbox, lưu toàn bộ query VNPay trả về rồi gửi nguyên query tới:

~~~text
GET  http://localhost:8080/api/payments/vnpay/callback?...query-tu-VNPay...
POST http://localhost:8080/api/payments/vnpay/ipn?...query-tu-VNPay...
~~~

Kỳ vọng:

- Browser callback trả 302 về frontend.
- IPN trả RspCode 00.
- Chỉ success khi vnp_ResponseCode=00 và vnp_TransactionStatus=00 theo [VNPay PAY API](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html).
- Payment PAID, order CONFIRMED, Saga COMPLETED.
- Gửi lại query không tạo transaction mới.

## 5. Luồng ngược và compensation

### R01 — Thiếu stock

~~~powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/orders/checkout/by-sku -Headers $headers -ContentType application/json -Body (@{ userId = $USER_ID; lines = @(@{ sku = $SKU; quantity = 999999 }); shippingAddress = $address; paymentMethod = 'COD' } | ConvertTo-Json -Depth 10)
~~~

Kỳ vọng: request fail, không tạo order/payment, stock không đổi.

### R02 — Payment fail

~~~powershell
$env:ALWAYS_FAIL = 'true'
docker compose up -d --build payment-service
~~~

Tạo COD order bằng F02 và chờ Saga.

Kỳ vọng: payment fail, order CANCELLED, Saga FAILED, stock về baseline, không double-release.

Khôi phục:

~~~powershell
$env:ALWAYS_FAIL = 'false'
docker compose up -d --build payment-service
~~~

### R03 — Customer cancel race

Tạo order rồi cancel ngay, không chờ Saga:

~~~powershell
$pending = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/orders/checkout/by-sku -Headers $headers -ContentType application/json -Body (@{ userId = $USER_ID; lines = @(@{ sku = $SKU; quantity = 1 }); shippingAddress = $address; paymentMethod = 'COD' } | ConvertTo-Json -Depth 10)
$cancelId = $pending.data.orderId
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/orders/$cancelId/cancel" -Headers $headers -ContentType application/json -Body (@{ reason = 'reverse-flow-test' } | ConvertTo-Json)
~~~

Kỳ vọng dù reserve chạy trước hay sau cancel: order CANCELLED, Saga FAILED, payment không thành công, stock về baseline.

### R04 — Staff cancel CONFIRMED

Dùng order COD sau F02 nhưng chưa SHIPPING:

~~~powershell
Invoke-RestMethod -Method PUT -Uri "http://localhost:8080/api/staff/orders/$ORDER_ID/status" -Headers $headers -ContentType application/json -Body (@{ status = 'CANCELLED'; reason = 'staff-reverse-flow-test' } | ConvertTo-Json)
~~~

Kỳ vọng: CONFIRMED → CANCELLED, release stock/voucher một lần. Không thể hủy SHIPPING hoặc DELIVERED.

### R05 — Late VNPay success

1. Tạo VNPAY order.
2. Reserve stock rồi hủy order trước callback.
3. Gửi callback success hợp lệ.

Kỳ vọng: order vẫn CANCELLED, không quay lại CONFIRMED; stock không release hai lần; nếu payment đã PAID thì phát payment.refund.

### R06 — Sai chữ ký

~~~powershell
Invoke-WebRequest -Method GET -Uri 'http://localhost:8080/api/payments/vnpay/callback?vnp_TxnRef=bad&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_SecureHash=bad' -MaximumRedirection 0 -SkipHttpErrorCheck
Invoke-RestMethod 'http://localhost:8080/api/payments/vnpay/ipn?vnp_TxnRef=bad&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_SecureHash=bad'
~~~

Kỳ vọng: không settle payment; IPN trả RspCode 97; không publish payment.result.

### R07 — Amount mismatch

Lấy query F06, thay vnp_Amount và ký lại toàn bộ params bằng VNPAY_HASH_SECRET trong Postman pre-request script.

Kỳ vọng khi chữ ký hợp lệ nhưng amount sai: IPN trả mã 04, payment FAILED, transaction error_code AMOUNT_MISMATCH, không confirm order.

### R08 — Response 00 nhưng transaction status không thành công

Ký query với:

~~~text
vnp_ResponseCode=00
vnp_TransactionStatus=02
~~~

Kỳ vọng: không PAID; transaction error_code VNPAY_STATUS_02; không confirm order.

### R09 — Callback/IPN trùng

Gửi cùng callback hai lần:

~~~powershell
docker exec cartzilla-postgres-pay-1 psql -U app -d cartzilla_pay_db -c "select provider, provider_txn_ref, count(*) from payment_transactions where provider = 'VNPAY' group by provider, provider_txn_ref having count(*) > 1;"
~~~

Kỳ vọng: không có provider_txn_ref trùng; lần hai idempotent.

### R10 — Refund VNPay và refund lặp

Với payment VNPAY PAID:

~~~powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/payments/$ORDER_ID/refund" -Headers $headers
~~~

Kỳ vọng:

- VNPAY_REFUND_ENABLED=true gọi Merchant API transaction type full refund 02 theo [VNPay Query/Refund API](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html).
- Response thành công hoặc duplicate/in-progress không tạo refund lần hai.
- Gọi lại endpoint không tăng refund transaction.
- Payment cuối REFUNDED.

Chưa có credential/giao dịch sandbox thì giữ VNPAY_REFUND_ENABLED=false và ghi rõ đây là simulated local refund; không dùng cấu hình này production.

### R11 — Voucher rollback retry

1. Tạo order có voucher.
2. Payment fail hoặc staff cancel sau redeem.
3. Gửi lại compensation/release.

Kỳ vọng: released=true, used_count giảm đúng một lần, không âm, retry idempotent.

### R12 — Auth, role và state transition

~~~powershell
Invoke-WebRequest -Method GET -Uri http://localhost:8080/api/orders -SkipHttpErrorCheck
Invoke-WebRequest -Method PUT -Uri "http://localhost:8080/api/staff/orders/$ORDER_ID/status" -ContentType application/json -Body (@{ status = 'CANCELLED'; reason = 'unauthorized' } | ConvertTo-Json) -SkipHttpErrorCheck
# Thử hủy DELIVERED hoặc chuyển DELIVERED -> PENDING
~~~

Kỳ vọng: 401/403/422 tùy lớp validation; không state mutation.

## 6. Bypass Gateway và IPN

~~~powershell
docker compose port user-service 8081
docker compose port product-service 8082
docker compose port order-service 8083
docker compose port payment-service 8084
Invoke-WebRequest http://localhost:8080/PRODUCT-SERVICE/api/products -SkipHttpErrorCheck
Invoke-RestMethod 'http://localhost:8080/api/payments/vnpay/ipn?vnp_TxnRef=x&vnp_SecureHash=bad'
~~~

Kỳ vọng:

- Port service là 127.0.0.1:808x, không phải 0.0.0.0:808x.
- /PRODUCT-SERVICE/** không auto-route.
- IPN không cần JWT nhưng vẫn verify chữ ký.

## 7. Queue và runtime

~~~powershell
docker exec cartzilla-rabbitmq-1 rabbitmqctl list_queues name messages consumers
docker compose logs --since 10m user-service product-service order-service payment-service api-gateway | Select-String 'ERROR|APPLICATION FAILED|FlywayValidateException|SQLState'
~~~

Kỳ vọng: có payment.refund.queue, stock.reserve.queue, stock.release.queue; không có startup/SQL/Flyway error mới; queue không tăng vô hạn.

## 8. Tiêu chí pass

| Nhóm | Bắt buộc |
|---|---|
| Xuôi | COD, COD delivered, voucher redeem, VNPay success |
| Ngược | thiếu stock, payment fail, customer cancel, staff cancel, late callback |
| Idempotency | callback, release, refund, voucher release lặp |
| Bảo mật | thiếu JWT, sai role, discovery bypass, secret không commit |
| Dữ liệu | stock không âm, không double-release, payment/Saga nhất quán |
| Vận hành | health 200, Flyway up-to-date, không log lỗi nghiêm trọng |

Release chỉ đạt khi toàn bộ case bắt buộc PASS và lưu lại orderId/response để truy vết.

