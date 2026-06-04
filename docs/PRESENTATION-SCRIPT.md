# Script Thuyết Trình — Domain Model & Clean Architecture (Cartzilla)

> Dùng cho phần **1.5 Aggregate, VO, Entities** + giải thích **Clean Architecture** trong cấu trúc thư mục.
> Thời lượng gợi ý: **8–10 phút**. Mỗi mục có [SLIDE] và lời thoại.

---

## PHẦN A — MỞ ĐẦU (≈1 phút)

> **[SLIDE: tiêu đề "1.5 Aggregate, VO, Entities"]**

"Chào thầy/cô và các bạn. Phần của em trình bày về cách **mô hình hoá miền nghiệp vụ** (domain) của hệ thống Cartzilla theo **Domain-Driven Design** — gọi tắt là DDD — và cách tổ chức nó vào cấu trúc thư mục theo **Clean Architecture**.

Trước khi đi vào từng service, em xin làm rõ 3 khái niệm cốt lõi, vì cả phần này xoay quanh chúng."

> **[SLIDE: 3 khái niệm]**

| Khái niệm | Định nghĩa ngắn | Cách nhận biết |
|---|---|---|
| **Entity (E)** | Đối tượng có **định danh riêng (ID)**, theo dõi qua thời gian | "Hai cái khác nhau dù mọi thuộc tính giống nhau" |
| **Value Object (VO)** | Đối tượng **bất biến**, so sánh **bằng giá trị**, không có ID | "Như tờ tiền 50k — đổi tờ nào cũng như nhau" |
| **Aggregate Root (AR)** | Một Entity đặc biệt làm **cổng vào duy nhất** của một cụm, đảm bảo **tính toàn vẹn (invariant)** | "Muốn đụng vào bên trong phải đi qua nó" |

"Một câu để phân biệt Entity và VO: **Tiền trong tài khoản** là Entity (có số tài khoản), còn **số tiền 50.000đ** là Value Object — không quan trọng là tờ nào, chỉ quan trọng giá trị."

---

## PHẦN B — VÌ SAO CẦN AGGREGATE (≈1 phút)

> **[SLIDE: hình ví dụ Order + OrderItem]**

"Tại sao cần Aggregate Root? Ví dụ đơn hàng `Order`. Một đơn có nhiều dòng hàng `OrderItem`. Quy tắc nghiệp vụ: **tổng tiền đơn = tổng các dòng**. Nếu cho phép sửa `OrderItem` tự do từ bên ngoài, ai đó có thể đổi số lượng mà quên cập nhật tổng tiền → dữ liệu sai lệch.

Giải pháp của DDD: gom `Order` + `OrderItem` thành một **Aggregate**, với `Order` là **Root**. Mọi thay đổi **bắt buộc đi qua method của `Order`** — ví dụ `order.addItem(...)` sẽ tự tính lại tổng. Nhờ vậy quy tắc nghiệp vụ (invariant) luôn được giữ.

Trong code của tụi em, điều này thể hiện ở chỗ constructor để `PROTECTED`, chỉ tạo qua factory `create()`, và các field chỉ đổi qua method có kiểm soát."

> **[SLIDE: code Order.java — đoạn factory + addItem]**

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // chặn new tự do
public class Order extends BaseEntity {
    public static Order create(...) { ... }          // cổng tạo
    public void confirm() { ... }                     // chuyển trạng thái có kiểm soát
    public void addItem(OrderItem item) {             // thêm dòng qua Root
        item.attachTo(this);
        this.items.add(item);
    }
}
```

---

## PHẦN C — ĐI QUA 5 SERVICE (≈4 phút)

"Hệ thống chia thành 5 **Bounded Context** — mỗi context là một service, một database riêng. Em đi nhanh qua domain model của từng cái."

### 1. user-service — Identity & Account
> **[SLIDE: bảng user-service]**

"Context này có **2 Aggregate Root độc lập**:
- **User** — gốc cho `Address` (địa chỉ giao hàng) và `RefreshToken`. Hai cái này là **Entity** vì có ID và vòng đời gắn với User.
- **Voucher** — mã giảm giá, tách riêng vì nó không thuộc về User cụ thể nào; `VoucherUsage` ghi lại mỗi lượt dùng.

Value Object ở đây: `Role` là enum (CUSTOMER/STAFF/ADMIN), `Email` (chuẩn hoá lowercase, validate định dạng), và `DiscountPolicy` gói loại giảm giá + giá trị + đơn tối thiểu.

**Invariant quan trọng:** email là duy nhất, và voucher chỉ hợp lệ khi còn lượt dùng, còn hạn, và đơn đạt giá trị tối thiểu."

### 2. product-service — Catalog (MongoDB)
> **[SLIDE: bảng product-service]**

"Đây là service duy nhất dùng **MongoDB**. Có 2 AR: **Product** và **Category**.

Điểm hay: **ProductVariant** — biến thể size/màu — là **Value Object được nhúng (embedded)** thẳng trong document Product, không tách collection riêng. Đúng tinh thần DDD: variant không có vòng đời độc lập, nó thuộc về Product.

VO khác: `Money` (số tiền chuẩn hoá BigDecimal), `ProductStatus` (ACTIVE/INACTIVE/DELETED), `Sku` (mã biến thể định dạng `<TÊN>-<MÀU>-<SIZE>`).

**Invariant:** tồn kho `stock ≥ 0` luôn đúng — mọi thao tác trừ kho phải đi qua method của Product, không set thẳng vào variant."

### 3. order-service — Ordering & Saga (giàu domain nhất)
> **[SLIDE: bảng order-service]**

"Đây là context **phức tạp nhất**, có **3 Aggregate Root**:
- **Cart** (giỏ hàng) + `CartItem`
- **Order** (đơn hàng) + `OrderItem` + `OrderStatusLog` — đây là transaction boundary chính
- **SagaState** — tách riêng vì nó quản lý **giao dịch phân tán** (distributed transaction) qua nhiều service. Tách ra để cập nhật độc lập, không khoá cả Order khi nhận event.

Nhiều VO dạng enum: `OrderStatus`, `PaymentMethod`, `SagaStep`, `SagaStatus`. Đặc biệt **ShippingAddress** là VO **snapshot** — sao chép địa chỉ tại thời điểm đặt hàng và **bất biến**, để sau này khách đổi địa chỉ trong hồ sơ thì đơn cũ không bị ảnh hưởng. Đây chính là quy tắc nghiệp vụ BR05."

### 4. payment-service — Payment
> **[SLIDE: bảng payment-service]**

"Một AR: **Payment** — mỗi giao dịch ứng với một order. VO gồm `Money`, `PaymentMethod`, `PaymentStatus` (PENDING/PAID/FAILED/REFUNDED), và `VnpayTransaction` gói kết quả trả về từ VNPay.

**Invariant:** một order chỉ có một Payment, và trạng thái chuyển theo luồng hợp lệ: PENDING → PAID/FAILED, PAID → REFUNDED."

### 5. notification-service — Notification
> **[SLIDE: bảng notification-service]**

"AR là **NotificationLog** — mỗi bản ghi là một email đã/đang gửi. VO: `NotificationType` (loại email), `NotificationStatus` (PENDING/SENT/FAILED), `EmailAddress`.

Service này nghe event `order.confirmed` / `order.cancelled` từ order-service rồi gửi mail."

> **[SLIDE: bảng tổng hợp]**

"Tổng cộng toàn hệ thống: **9 Aggregate Root, 6 Entity, khoảng 18 Value Object.**"

---

## PHẦN D — CLEAN ARCHITECTURE TRONG CẤU TRÚC THƯ MỤC (≈3 phút)

> **[SLIDE: 4 vòng tròn Clean Architecture]**

"Phần cuối — và quan trọng nhất — là **các domain object này nằm ở đâu trong code**. Tụi em tổ chức mỗi service theo **Clean Architecture** (còn gọi Hexagonal / Ports & Adapters).

Ý tưởng cốt lõi của Clean Architecture là **Quy tắc phụ thuộc** (Dependency Rule): **mọi phụ thuộc chỉ hướng VÀO TRONG**. Tầng ngoài biết tầng trong, tầng trong **không biết gì** về tầng ngoài."

> **[SLIDE: cây thư mục 1 service]**

```
services/order-service/src/main/java/com/cartzilla/order/
├── api/              ← Tầng ngoài cùng: REST controller, DTO
├── application/      ← Use case: điều phối nghiệp vụ
├── domain/          ← LÕI: aggregate, entity, vo, repository (interface)
└── infrastructure/   ← Tầng ngoài: JPA, RabbitMQ, adapter
```

"Em map 4 thư mục này với 4 vòng của Clean Architecture:"

> **[SLIDE: bảng map]**

| Thư mục | Vai trò Clean Architecture | Chứa gì | Phụ thuộc |
|---|---|---|---|
| **domain/** | **Entities (lõi trong cùng)** | Aggregate, Entity, VO, **Port** (interface repository) | **Không phụ thuộc gì** — không import Spring, không JPA |
| **application/** | **Use Cases** | `CheckoutUseCase`, `RegisterUserUseCase`... | Chỉ phụ thuộc `domain` |
| **api/** | **Interface Adapters (vào)** | Controller, Request/Response DTO | Gọi `application` |
| **infrastructure/** | **Frameworks & Drivers (ra)** | JPA repo, RabbitMQ, `RepositoryAdapter` | **Implement** interface của `domain` |

"Điểm mấu chốt nằm ở chữ **Port & Adapter**:

- Trong `domain/repository/` em định nghĩa **interface** `OrderRepository` — đây là **Port**. Domain chỉ nói *'tôi cần lưu order'* mà **không quan tâm lưu bằng gì**.
- Trong `infrastructure/adapter/` mới có `OrderRepositoryAdapter` **implement** interface đó bằng JPA.

Nhờ vậy **mũi tên phụ thuộc bị đảo ngược** (Dependency Inversion): bình thường domain sẽ phải phụ thuộc database, nhưng ở đây **database (infrastructure) phụ thuộc ngược vào domain**."

> **[SLIDE: sơ đồ luồng]**

```
HTTP → api/Controller → application/UseCase → domain/Aggregate
                                                   ↓ qua Port (interface)
                                          domain/repository (interface)
                                                   ↑ implement
                                          infrastructure/adapter → JPA → DB

         Phụ thuộc:  api → application → domain ← infrastructure
                                          (LÕI)
```

### Vì sao điều này có lợi?

> **[SLIDE: 3 lợi ích]**

"Ba lợi ích thực tế:

1. **Domain thuần khiết, dễ test** — vì `domain` không import Spring hay JPA, em test logic nghiệp vụ (vd 'tổng tiền = subtotal − discount') mà không cần khởi động database.

2. **Thay công nghệ không ảnh hưởng nghiệp vụ** — nếu mai đổi từ PostgreSQL sang MongoDB, em chỉ sửa `infrastructure/`, còn `domain/` và `application/` **giữ nguyên**. Thực tế trong dự án: product-service dùng MongoDB, các service khác dùng PostgreSQL, nhưng **cấu trúc domain y hệt nhau**.

3. **Phân chia công việc rõ ràng** — mỗi thành viên sở hữu một service, và trong service ai đụng tầng nào biết rõ ranh giới."

---

## PHẦN E — KẾT (≈30 giây)

> **[SLIDE: tóm tắt]**

"Tóm lại, phần của em gồm 2 ý:

1. **Mô hình hoá domain** bằng DDD: phân biệt rõ Aggregate Root, Entity, Value Object cho cả 5 service — 9 AR, 6 Entity, 18 VO.

2. **Đặt domain đó vào Clean Architecture**: tách 4 tầng `api / application / domain / infrastructure`, với domain là lõi không phụ thuộc framework, và dùng Port–Adapter để đảo ngược phụ thuộc.

Kết quả là một codebase **dễ test, dễ bảo trì, và độc lập với công nghệ**. Em xin hết phần trình bày, cảm ơn thầy/cô và các bạn."

---

## PHỤ LỤC — Câu hỏi có thể bị hỏi & cách trả lời

| Câu hỏi | Trả lời ngắn |
|---|---|
| "Sao SagaState tách AR riêng mà không nằm trong Order?" | Vì saga cập nhật bất đồng bộ qua event nhiều lần; tách ra để không khoá cả Order aggregate mỗi lần nhận event, và saga có vòng đời/transaction riêng. |
| "ShippingAddress là VO nhưng sao lưu trong bảng orders?" | Nó là VO snapshot — nhúng (embedded) dưới dạng JSONB trong orders, bất biến. Không tham chiếu bảng addresses để khách đổi địa chỉ không ảnh hưởng đơn cũ (BR05). |
| "Money lặp ở nhiều service, sao không để chung?" | Mỗi Bounded Context độc lập nên định nghĩa VO riêng — đánh đổi giữa DRY và độc lập context. Có thể đưa vào shared/common-web nếu ưu tiên tái sử dụng. |
| "Domain không phụ thuộc Spring thật không?" | Đúng — domain chỉ dùng JPA annotation (chuẩn Jakarta) cho mapping; logic nghiệp vụ thuần Java, repository là interface. Adapter ở infrastructure mới biết Spring Data. |
| "Khác gì kiến trúc 3 lớp (Controller-Service-Repository) cũ?" | 3 lớp truyền thống: Service phụ thuộc Repository cụ thể (hướng ra DB). Clean Arch: domain định nghĩa interface, infrastructure implement → phụ thuộc đảo ngược, domain là trung tâm chứ không phải DB. |

---

*Script thuyết trình Cartzilla · Domain Model (DDD) + Clean Architecture · 8–10 phút*
