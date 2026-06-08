# Database Design — Cartzilla Microservices

> **Căn chỉnh theo repo `SE1911-JV_MSS301`:** Database-per-Service · mỗi entity kế thừa **`BaseEntity`** (`shared/common-web`) → tự có cột audit `created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted`, `deleted_at`. Soft delete qua `@SQLRestriction("is_deleted = false")`. Package `com.cartzilla.<service>`. JPA `ddl-auto: update` ở dev, Flyway ở prod.

---

## 0. BaseEntity — cột chung mọi bảng JPA

Trích từ `shared/common-web/.../base/BaseEntity.java` (đã có trong repo):

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate      private Instant createdAt;     // created_at  NOT NULL, updatable=false
    @LastModifiedDate private Instant updatedAt;     // updated_at
    @CreatedBy        private String  createdBy;     // created_by  (từ AuditorAware)
    @LastModifiedBy   private String  updatedBy;     // updated_by
    private boolean deleted = false;                 // is_deleted  NOT NULL
    private Instant deletedAt;                        // deleted_at
    public void softDelete() { ... }
}
```

→ Mọi bảng PostgreSQL dưới đây **ngầm có thêm 6 cột**: `created_at, updated_at, created_by, updated_by, is_deleted, deleted_at`. Phần DDL chỉ liệt kê cột nghiệp vụ + ghi `-- + BaseEntity columns`.

---

## Tổng quan — Database per Service

```
┌──────────────────────┬───────────────────────┬─────────────────┬────────┐
│ Service              │ Database              │ Technology      │ Port   │
├──────────────────────┼───────────────────────┼─────────────────┼────────┤
│ user-service         │ cartzilla_user_db     │ PostgreSQL 16   │ 8081   │
│ product-service      │ cartzilla_product_db  │ PostgreSQL 16   │ 8082   │
│ order-service        │ cartzilla_order_db    │ PostgreSQL 16   │ 8083   │
│ payment-service      │ cartzilla_pay_db      │ PostgreSQL 16   │ 8084   │
│ notification-service │ cartzilla_notif_db    │ PostgreSQL 16   │ 8085   │
└──────────────────────┴───────────────────────┴─────────────────┴────────┘
```

> Mỗi service kết nối DB riêng (theo `application.yml` repo: `jdbc:postgresql://localhost:5432/<db>`). Không JOIN cross-service.

---

## 1. cartzilla_user_db (PostgreSQL) — `user-service` (Dev2)

### ERD

```
┌─────────────────────────────┐         ┌─────────────────────────────┐
│           users             │ 1     N │         addresses           │
├─────────────────────────────┤────────>├─────────────────────────────┤
│ id          UUID  PK        │         │ id          UUID  PK        │
│ email       VARCHAR(255) UK │         │ user_id     UUID  FK        │
│ password_hash VARCHAR(255)  │         │ full_name   VARCHAR(100)    │
│ full_name   VARCHAR(100)    │         │ phone       VARCHAR(20)     │
│ phone       VARCHAR(20)     │         │ street      VARCHAR(255)    │
│ role        VARCHAR(20)     │         │ district    VARCHAR(100)    │
│ email_verified BOOLEAN      │         │ city        VARCHAR(100)    │
│ is_active   BOOLEAN         │         │ is_default  BOOLEAN         │
│ + BaseEntity columns        │         │ + BaseEntity columns        │
└─────────────────────────────┘         └─────────────────────────────┘
       │ 1
       │ N
       ▼
┌─────────────────────────────┐         ┌──────────────────────────────┐
│       refresh_tokens        │         │           vouchers           │ 1
├─────────────────────────────┤         ├──────────────────────────────┤──┐
│ id          UUID  PK        │         │ id           UUID  PK        │  │
│ user_id     UUID  FK        │         │ code         VARCHAR(50) UK* │  │
│ token       TEXT  UK        │         │ discount_type VARCHAR(20)    │  │
│ expires_at  TIMESTAMP       │         │ discount_value DECIMAL(10,2) │  │
│ + BaseEntity columns        │         │ max_discount_amount DECIMAL  │  │
└─────────────────────────────┘         │ min_order_amount DECIMAL     │  │
                                        │ min_account_age_days INT     │  │
                                        │ per_user_limit INTEGER       │  │
                                        │ audience_type VARCHAR(30)    │  │
                                        │ first_order_only BOOLEAN     │  │
                                        │ min_completed_orders INT     │  │
                                        │ min_total_spent DECIMAL      │  │
                                        │ max_uses     INTEGER         │  │
                                        │ used_count   INTEGER         │  │
                                        │ is_active    BOOLEAN         │  │
                                        │ starts_at    TIMESTAMP       │  │
                                        │ expires_at   TIMESTAMP       │  │
                                        │ + BaseEntity columns         │  │
                                        └──────────────────────────────┘  │
                                                  │ 1          │ 1        │
                                                  │ N          │ N        │
                                                  ▼            ▼          │
                                        ┌────────────────────┐ ┌─────────────────────┐
                                        │   voucher_usages   │ │ voucher_allowed_users│
                                        ├────────────────────┤ ├─────────────────────┤
                                        │ id UUID PK         │ │ id UUID PK          │
                                        │ voucher_id UUID FK │ │ voucher_id UUID FK  │
                                        │ user_id UUID (ref) │ │ user_id UUID (ref)  │
                                        │ order_id UUID(ref) │ │ + BaseEntity cols   │
                                        │ + BaseEntity cols  │ └─────────────────────┘
                                        └────────────────────┘

* `vouchers.code` unique không phân biệt hoa thường qua `uq_vouchers_code_upper = unique(upper(code))`.
* `voucher_usages` unique (`voucher_id`, `order_id`) để redeem idempotent theo order.
* `voucher_allowed_users` chỉ dùng cho `audience_type = SPECIFIC_USERS`, unique (`voucher_id`, `user_id`).
```

OAuth bổ sung:

```
┌─────────────────────────────┐ 1     N ┌─────────────────────────────┐
│           users             │────────>│        oauth_accounts       │
├─────────────────────────────┤         ├─────────────────────────────┤
│ id          UUID PK         │         │ id          UUID PK         │
│ email       VARCHAR(255) UK │         │ user_id     UUID FK         │
│ + BaseEntity columns        │         │ provider    VARCHAR(30)     │
└─────────────────────────────┘         │ provider_user_id VARCHAR    │
                                        │ provider_email VARCHAR(255) │
                                        │ linked_at    TIMESTAMP      │
                                        │ + BaseEntity columns        │
                                        └─────────────────────────────┘
```

Enums: `oauth_accounts.provider = GOOGLE|FACEBOOK`.

### Domain mapping (DDD)
- **Aggregate root:** `UserAggregate` (chứa `User` + `List<Address>`)
- **Entity:** `User`, `Address`, `RefreshToken`, `OAuthAccount`, `Voucher`, `VoucherUsage`, `VoucherAllowedUser`
- **VO:** `Email`, `Role` (enum: CUSTOMER, STAFF, ADMIN), `OAuthProvider` (GOOGLE, FACEBOOK), `AccountTenure` (số ngày kể từ `users.created_at`)
- **Port:** `UserRepository`, `OAuthAccountRepository`, `VoucherRepository` (domain) → adapter ở `infrastructure`

### DDL — Flyway `V1__init.sql`

```sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),                           -- nullable nếu user chỉ đăng nhập OAuth
    full_name     VARCHAR(100),
    phone         VARCHAR(20),
    role          VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',  -- CUSTOMER|STAFF|ADMIN
    email_verified BOOLEAN NOT NULL DEFAULT false,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    -- BaseEntity
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    is_deleted    BOOLEAN NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP
);

CREATE TABLE addresses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    street      VARCHAR(255) NOT NULL,
    district    VARCHAR(100) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token       TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE oauth_accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    provider         VARCHAR(30) NOT NULL,                  -- GOOGLE|FACEBOOK
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email   VARCHAR(255),
    display_name     VARCHAR(100),
    avatar_url       TEXT,
    linked_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at    TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by       VARCHAR(255), updated_by VARCHAR(255),
    is_deleted       BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    UNIQUE (provider, provider_user_id),
    UNIQUE (user_id, provider)
);

CREATE TABLE vouchers (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                 VARCHAR(50) NOT NULL,        -- mã voucher admin nhập; app normalize uppercase, DB unique theo upper(code)
    discount_type        VARCHAR(20) NOT NULL,        -- PERCENTAGE|FIXED_AMOUNT
    discount_value       DECIMAL(10,2) NOT NULL,      -- % nếu PERCENTAGE, số tiền VND nếu FIXED_AMOUNT
    max_discount_amount  DECIMAL(12,2),               -- trần tiền giảm; bắt buộc > 0 với PERCENTAGE để tránh giảm quá lớn
    min_order_amount     DECIMAL(12,2) NOT NULL DEFAULT 0, -- subtotal tối thiểu để được áp voucher
    min_account_age_days INTEGER NOT NULL DEFAULT 0 CHECK (min_account_age_days >= 0), -- tuổi tài khoản tối thiểu; 0 = không giới hạn
    per_user_limit       INTEGER NOT NULL DEFAULT 1,  -- số lần tối đa một user được redeem voucher này
    audience_type        VARCHAR(30) NOT NULL DEFAULT 'ALL_USERS', -- ALL_USERS|NEW_CUSTOMER|LOYAL_CUSTOMER|SPECIFIC_USERS
    first_order_only     BOOLEAN NOT NULL DEFAULT false, -- true nếu chỉ cho user chưa có đơn hoàn tất
    min_completed_orders INTEGER NOT NULL DEFAULT 0,  -- số đơn hoàn tất tối thiểu cho loyalty voucher
    min_total_spent      DECIMAL(12,2) NOT NULL DEFAULT 0, -- tổng chi tiêu tối thiểu cho loyalty voucher
    max_uses             INTEGER NOT NULL DEFAULT 1,  -- tổng số lượt redeem toàn hệ thống
    used_count           INTEGER NOT NULL DEFAULT 0,  -- số lượt đã redeem thành công; tăng atomically khi commit voucher
    is_active            BOOLEAN NOT NULL DEFAULT true, -- admin bật/tắt voucher
    starts_at            TIMESTAMP,                   -- thời điểm bắt đầu hiệu lực; NULL = hiệu lực ngay
    expires_at           TIMESTAMP,                   -- thời điểm hết hạn; NULL = không hết hạn
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by           VARCHAR(255), updated_by VARCHAR(255),
    is_deleted           BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CHECK (
        (discount_type = 'PERCENTAGE' AND discount_value > 0 AND discount_value <= 100 AND max_discount_amount IS NOT NULL AND max_discount_amount > 0)
        OR (discount_type = 'FIXED_AMOUNT' AND discount_value > 0)
    ),
    CHECK (min_order_amount >= 0),
    CHECK (per_user_limit >= 1),
    CHECK (min_completed_orders >= 0),
    CHECK (min_total_spent >= 0),
    CHECK (max_uses >= 1),
    CHECK (used_count >= 0 AND used_count <= max_uses),
    CHECK (audience_type IN ('ALL_USERS', 'NEW_CUSTOMER', 'LOYAL_CUSTOMER', 'SPECIFIC_USERS')),
    CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at > starts_at)
);

CREATE TABLE voucher_usages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id  UUID NOT NULL REFERENCES vouchers(id), -- voucher được redeem
    user_id     UUID NOT NULL,    -- ref users.id; không FK cross-service
    order_id    UUID NOT NULL,    -- ref orders.id; không FK cross-service
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    UNIQUE (voucher_id, order_id) -- idempotent redeem: retry cùng order không tăng used_count lần hai
);

CREATE TABLE voucher_allowed_users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id  UUID NOT NULL REFERENCES vouchers(id), -- chỉ dùng khi vouchers.audience_type = SPECIFIC_USERS
    user_id     UUID NOT NULL,    -- ref users.id; user được phép dùng voucher này
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    UNIQUE (voucher_id, user_id) -- tránh duplicate cùng user trong whitelist voucher
);

CREATE INDEX idx_users_email        ON users(email);
CREATE INDEX idx_addresses_user     ON addresses(user_id);
CREATE INDEX idx_refresh_tokens_tok ON refresh_tokens(token);
CREATE INDEX idx_oauth_user         ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_provider     ON oauth_accounts(provider, provider_user_id);
CREATE UNIQUE INDEX uq_vouchers_code_upper ON vouchers(upper(code));
CREATE INDEX idx_voucher_usages_v   ON voucher_usages(voucher_id);
CREATE INDEX idx_voucher_usages_user ON voucher_usages(voucher_id, user_id);
CREATE INDEX idx_voucher_allowed_users_v ON voucher_allowed_users(voucher_id);
```

**Flyway `V2__vouchers_min_account_age_days.sql`** (nếu đã deploy V1):

```sql
ALTER TABLE vouchers
    ADD COLUMN IF NOT EXISTS min_account_age_days INTEGER NOT NULL DEFAULT 0
    CHECK (min_account_age_days >= 0);

COMMENT ON COLUMN vouchers.min_account_age_days IS
    'Số ngày tối thiểu kể từ users.created_at; user chỉ áp voucher khi accountAgeDays >= giá trị này. 0 = không giới hạn.';
```

**Flyway `V3__voucher_business_rules.sql`** (nếu đã deploy V1/V2):

```sql
ALTER TABLE vouchers
    ADD COLUMN IF NOT EXISTS max_discount_amount DECIMAL(12,2), -- trần tiền giảm; bắt buộc với PERCENTAGE
    ADD COLUMN IF NOT EXISTS per_user_limit INTEGER NOT NULL DEFAULT 1, -- số lần tối đa một user được redeem
    ADD COLUMN IF NOT EXISTS audience_type VARCHAR(30) NOT NULL DEFAULT 'ALL_USERS', -- nhóm user được dùng voucher
    ADD COLUMN IF NOT EXISTS first_order_only BOOLEAN NOT NULL DEFAULT false, -- chỉ cho user chưa có đơn hoàn tất
    ADD COLUMN IF NOT EXISTS min_completed_orders INTEGER NOT NULL DEFAULT 0, -- điều kiện loyalty theo số đơn
    ADD COLUMN IF NOT EXISTS min_total_spent DECIMAL(12,2) NOT NULL DEFAULT 0, -- điều kiện loyalty theo tổng chi tiêu
    ADD COLUMN IF NOT EXISTS starts_at TIMESTAMP; -- thời điểm bắt đầu hiệu lực

-- Backfill tạm cho percentage voucher cũ trước khi thêm constraint; admin nên rà lại giá trị theo campaign.
UPDATE vouchers
SET max_discount_amount = 999999999
WHERE discount_type = 'PERCENTAGE' AND max_discount_amount IS NULL;

ALTER TABLE vouchers
    DROP CONSTRAINT IF EXISTS vouchers_code_key;

ALTER TABLE vouchers
    ADD CONSTRAINT chk_vouchers_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    ADD CONSTRAINT chk_vouchers_discount_value CHECK (
        (discount_type = 'PERCENTAGE' AND discount_value > 0 AND discount_value <= 100 AND max_discount_amount IS NOT NULL AND max_discount_amount > 0)
        OR (discount_type = 'FIXED_AMOUNT' AND discount_value > 0)
    ),
    ADD CONSTRAINT chk_vouchers_limits CHECK (
        min_order_amount >= 0
        AND min_account_age_days >= 0
        AND per_user_limit >= 1
        AND min_completed_orders >= 0
        AND min_total_spent >= 0
        AND max_uses >= 1
        AND used_count >= 0
        AND used_count <= max_uses
    ),
    ADD CONSTRAINT chk_vouchers_audience_type CHECK (audience_type IN ('ALL_USERS', 'NEW_CUSTOMER', 'LOYAL_CUSTOMER', 'SPECIFIC_USERS')),
    ADD CONSTRAINT chk_vouchers_time_window CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at > starts_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vouchers_code_upper ON vouchers(upper(code));

ALTER TABLE voucher_usages
    ADD CONSTRAINT uq_voucher_usage_order UNIQUE (voucher_id, order_id);

CREATE INDEX IF NOT EXISTS idx_voucher_usages_user ON voucher_usages(voucher_id, user_id);

CREATE TABLE IF NOT EXISTS voucher_allowed_users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id  UUID NOT NULL REFERENCES vouchers(id), -- chỉ dùng khi vouchers.audience_type = SPECIFIC_USERS
    user_id     UUID NOT NULL, -- ref users.id; user được whitelist
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    UNIQUE (voucher_id, user_id) -- tránh duplicate whitelist
);
```

**Logic áp dụng (application layer):**

```java
long accountAgeDays = ChronoUnit.DAYS.between(
    user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
    Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
);
if (accountAgeDays < voucher.getMinAccountAgeDays()) {
    throw new VoucherNotEligibleException("VA-06", voucher.getMinAccountAgeDays(), accountAgeDays);
}

// Validate/preview không tăng usedCount.
// Redeem/commit phải dùng conditional update để chống oversell:
// UPDATE vouchers
// SET used_count = used_count + 1
// WHERE id = :voucherId AND used_count < max_uses;
```

---

## 2. cartzilla_product_db (PostgreSQL) — `product-service` (Dev3)

> Quyết định cập nhật: product-service chuyển sang PostgreSQL 16 để thống nhất JPA/Flyway/BaseEntity với các service còn lại. `order-service` vẫn lưu snapshot tên/ảnh/giá tại thời điểm đặt hàng, không JOIN trực tiếp sang product DB.

### ERD

```
┌─────────────────────────────┐ 1     N ┌─────────────────────────────┐
│         categories          │────────>│          products           │
├─────────────────────────────┤         ├─────────────────────────────┤
│ id UUID PK                  │         │ id UUID PK                  │
│ parent_id UUID FK nullable  │         │ category_id UUID FK         │
│ name VARCHAR(100)           │         │ vendor_id UUID FK nullable  │
│ slug VARCHAR(120) UK        │         │ name VARCHAR(200)           │
│ image_url TEXT              │         │ slug VARCHAR(220) UK        │
│ active BOOLEAN              │         │ base_price DECIMAL(12,2)    │
│ sort_order INTEGER          │         │ active · featured BOOLEAN   │
│ + BaseEntity columns        │         │ tags TEXT[]                 │
└─────────────────────────────┘         │ + BaseEntity columns        │
                                        └──────────┬──────────────────┘
                                                   │ 1
┌─────────────────────────────┐ 1     N            │ N
│           vendors           │──────────────┐     ▼
├─────────────────────────────┤              │ ┌─────────────────────────────┐
│ id UUID PK                  │              └>│       product_variants      │
│ name VARCHAR(150)           │                ├─────────────────────────────┤
│ slug VARCHAR(160) UK        │                │ id UUID PK · product_id FK  │
│ vendor_type VARCHAR(20)     │                │ sku VARCHAR(50) UK          │
│ contact_email VARCHAR(255)  │                │ size · color · color_hex    │
│ website TEXT                │                │ price DECIMAL · stock INT   │
│ active BOOLEAN              │                │ + BaseEntity columns        │
│ + BaseEntity columns        │                └─────────────────────────────┘
└─────────────────────────────┘

┌─────────────────────────────┐
│        product_images       │
├─────────────────────────────┤
│ id UUID PK · product_id FK  │
│ image_url TEXT              │
│ alt_text VARCHAR(200)       │
│ is_primary BOOLEAN          │
│ sort_order INTEGER          │
│ + BaseEntity columns        │
└─────────────────────────────┘
```

### Domain mapping (DDD)
- **Aggregate root:** `ProductAggregate` (Product + variants/images, invariant: tổng stock ≥ 0)
- **Entity:** `Product`, `Category`, `Vendor`, `ProductVariant`, `ProductImage`
- **VO:** `Money`, `VendorType` (SUPPLIER, BRAND, MANUFACTURER)
- **Port:** `ProductRepository`, `CategoryRepository`, `VendorRepository` → adapter dùng `JpaRepository`

### DDL — Flyway `V1__init.sql`

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id UUID REFERENCES categories(id),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    image_url TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL UNIQUE,
    vendor_type VARCHAR(20) NOT NULL DEFAULT 'BRAND',       -- SUPPLIER|BRAND|MANUFACTURER
    contact_email VARCHAR(255),
    phone VARCHAR(20),
    website TEXT,
    logo_url TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES categories(id),
    vendor_id UUID REFERENCES vendors(id),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    description TEXT,
    base_price DECIMAL(12,2) NOT NULL CHECK (base_price >= 0),
    tags TEXT[],
    active BOOLEAN NOT NULL DEFAULT true,
    featured BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    sku VARCHAR(50) NOT NULL UNIQUE,
    size VARCHAR(20),
    color VARCHAR(50),
    color_hex VARCHAR(10),
    price DECIMAL(12,2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    image_url TEXT NOT NULL,
    alt_text VARCHAR(200),
    is_primary BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE INDEX idx_categories_parent       ON categories(parent_id);
CREATE INDEX idx_vendors_active          ON vendors(active);
CREATE INDEX idx_products_category       ON products(category_id, active);
CREATE INDEX idx_products_vendor         ON products(vendor_id, active);
CREATE INDEX idx_products_featured       ON products(featured, active);
CREATE INDEX idx_product_variants_prod   ON product_variants(product_id);
CREATE INDEX idx_product_images_prod     ON product_images(product_id, sort_order);
CREATE INDEX idx_products_tags           ON products USING GIN (tags);
CREATE INDEX idx_products_search         ON products USING GIN (
    to_tsvector('simple', coalesce(name, '') || ' ' || coalesce(description, ''))
);
```

---

## 3. cartzilla_order_db (PostgreSQL) — `order-service` (Dev4)

### ERD

```
┌─────────────────────────────┐
│        cart_items           │   (UNIQUE user_id + sku)
├─────────────────────────────┤
│ id UUID PK · user_id UUID   │
│ product_id UUID (ref)       │  ← product-service products.id
│ sku · name · image          │
│ size · color · price        │
│ quantity INTEGER            │
│ + BaseEntity columns        │
└─────────────────────────────┘

┌─────────────────────────────┐ 1     N ┌─────────────────────────────┐
│           orders            │────────>│        order_items          │
├─────────────────────────────┤         ├─────────────────────────────┤
│ id            UUID PK       │         │ id UUID PK · order_id FK    │
│ user_id       UUID (ref)    │         │ product_id UUID (ref)       │
│ status        VARCHAR(20)   │         │ sku · name · image          │
│ payment_method VARCHAR(20)  │         │ size · color                │
│ payment_status VARCHAR(20)  │         │ unit_price DECIMAL(12,2)    │
│ subtotal      DECIMAL(12,2) │         │ quantity INTEGER            │
│ discount      DECIMAL(12,2) │         │ subtotal DECIMAL(12,2)      │
│ total_amount  DECIMAL(12,2) │         └─────────────────────────────┘
│ voucher_code  VARCHAR(50)   │ 1
│ shipping_address JSONB      │ │ N     ┌─────────────────────────────┐
│ note · cancelled_reason     │ └──────>│      order_status_logs      │
│ confirmed_at  TIMESTAMP     │         ├─────────────────────────────┤
│ + BaseEntity columns        │         │ id · order_id FK            │
└─────────────────────────────┘         │ from_status · to_status     │
                                        │ changed_by UUID · note      │
┌─────────────────────────────┐         │ + BaseEntity columns        │
│         saga_states         │         └─────────────────────────────┘
├─────────────────────────────┤
│ id UUID PK · order_id UK    │
│ current_step VARCHAR(20)    │  RESERVE_STOCK|PROCESS_PAYMENT|NOTIFY|DONE
│ status       VARCHAR(20)    │  IN_PROGRESS|COMPLETED|FAILED
│ retry_count INTEGER         │
│ error_message TEXT          │
│ + BaseEntity columns        │
└─────────────────────────────┘
```

### Domain mapping (DDD)
- **Aggregate:** `CartAggregate` (CartItem), `OrderAggregate` (Order + OrderItem + invariant tổng tiền)
- **Entity:** `CartItem`, `Order`, `OrderItem`, `OrderStatusLog`, `SagaState`
- **VO:** `ShippingAddress` (lưu JSONB snapshot), `OrderStatus`, `Money`
- **Port:** `CartRepository`, `OrderRepository`, `SagaStateRepository`
- **Specification:** `OrderSearchCriteria` (filter staff orders theo status/date) — như `JobSearchCriteria` repo

### DDL — Flyway `V1__init.sql`

```sql
CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    image TEXT, size VARCHAR(20), color VARCHAR(50),
    price DECIMAL(12,2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    UNIQUE (user_id, sku)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',         -- PENDING|CONFIRMED|SHIPPING|DELIVERED|CANCELLED
    payment_method VARCHAR(20) NOT NULL,                   -- COD|VNPAY
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING|PAID|FAILED|REFUNDED
    subtotal DECIMAL(12,2) NOT NULL,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    voucher_code VARCHAR(50),
    shipping_address JSONB NOT NULL,                       -- snapshot
    note TEXT, cancelled_reason TEXT, confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    product_id UUID NOT NULL,
    sku VARCHAR(50) NOT NULL, name VARCHAR(200) NOT NULL,
    image TEXT, size VARCHAR(20), color VARCHAR(50),
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE order_status_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    from_status VARCHAR(20), to_status VARCHAR(20) NOT NULL,
    changed_by UUID, note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE saga_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    current_step VARCHAR(20) NOT NULL DEFAULT 'RESERVE_STOCK',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE INDEX idx_cart_user      ON cart_items(user_id);
CREATE INDEX idx_orders_user    ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);
CREATE INDEX idx_order_items_o  ON order_items(order_id);
CREATE INDEX idx_saga_order     ON saga_states(order_id);
CREATE INDEX idx_order_logs_o   ON order_status_logs(order_id);
```

### shipping_address JSONB (snapshot — BR05)

```json
{ "fullName": "Nguyễn Văn A", "phone": "0901234567",
  "street": "123 Lê Lợi", "district": "Quận 1", "city": "TP.HCM" }
```

---

## 4. cartzilla_pay_db (PostgreSQL) — `payment-service` (Dev1)

### ERD

```
┌─────────────────────────────┐ 1     N ┌─────────────────────────────┐
│          payments           │────────>│    payment_transactions     │
├─────────────────────────────┤         ├─────────────────────────────┤
│ id            UUID PK       │         │ id UUID PK                  │
│ order_id      UUID UK (ref) │         │ payment_id UUID FK          │
│ user_id       UUID (ref)    │         │ transaction_type VARCHAR(30)│
│ method        VARCHAR(20)   │         │ provider VARCHAR(20)        │
│ amount        DECIMAL(12,2) │         │ provider_txn_ref VARCHAR    │
│ status        VARCHAR(20)   │         │ amount DECIMAL(12,2)        │
│ vnpay_txn_ref VARCHAR(100)  │         │ status VARCHAR(20)          │
│ vnpay_response JSONB        │         │ request_payload JSONB       │
│ paid_at       TIMESTAMP     │         │ response_payload JSONB      │
│ + BaseEntity columns        │         │ processed_at TIMESTAMP      │
└─────────────────────────────┘         │ + BaseEntity columns        │
                                        └─────────────────────────────┘
```

Enums: `method/provider = COD|VNPAY`, `transaction_type = INIT|PAY|VERIFY|REFUND|CALLBACK`, `payment_transactions.status = PENDING|SUCCESS|FAILED`.

### Domain mapping
- **Entity:** `Payment`, `PaymentTransaction`
- **VO:** `PaymentMethod`, `PaymentStatus`, `TransactionType`
- **Port:** `PaymentRepository`, `PaymentTransactionRepository`

### DDL — Flyway `V1__init.sql`

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    method VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    vnpay_txn_ref VARCHAR(100),
    vnpay_response JSONB,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    order_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,                  -- INIT|PAY|VERIFY|REFUND|CALLBACK
    provider VARCHAR(20) NOT NULL,                          -- COD|VNPAY
    provider_txn_ref VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',          -- PENDING|SUCCESS|FAILED
    request_payload JSONB,
    response_payload JSONB,
    error_code VARCHAR(50),
    error_message TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payment_txn_payment ON payment_transactions(payment_id);
CREATE INDEX idx_payment_txn_order ON payment_transactions(order_id);
CREATE UNIQUE INDEX uq_payment_txn_provider_ref
    ON payment_transactions(provider, provider_txn_ref)
    WHERE provider_txn_ref IS NOT NULL;
```

---

## 5. cartzilla_notif_db (PostgreSQL) — `notification-service` (Dev1)

> Cập nhật: tách rõ thông báo trong hệ thống (`notifications`) và email gửi ra ngoài (`email_logs`). Email có thể gắn với một notification qua `notification_id`, hoặc tồn tại độc lập cho luồng như reset password.

### ERD

```
┌─────────────────────────────┐ 1     N ┌─────────────────────────────┐
│        notifications        │────────>│          email_logs         │
├─────────────────────────────┤         ├─────────────────────────────┤
│ id UUID PK                  │         │ id UUID PK                  │
│ recipient_user_id UUID ref  │         │ notification_id UUID FK     │
│ order_id UUID ref nullable  │         │ order_id UUID ref nullable  │
│ type VARCHAR(30)            │         │ recipient_email VARCHAR(255)│
│ title VARCHAR(255)          │         │ subject VARCHAR(255)        │
│ message TEXT                │         │ template_key VARCHAR(100)   │
│ status VARCHAR(20)          │         │ provider VARCHAR(50)        │
│ priority VARCHAR(20)        │         │ status VARCHAR(20)          │
│ read_at TIMESTAMP           │         │ sent_at TIMESTAMP           │
│ data JSONB                  │         │ error TEXT                  │
│ + BaseEntity columns        │         │ + BaseEntity columns        │
└─────────────────────────────┘         └─────────────────────────────┘
```

Enums: `notifications.status = UNREAD|READ|ARCHIVED`, `notifications.priority = LOW|NORMAL|HIGH`, `email_logs.status = PENDING|SENT|FAILED`.

### Domain mapping
- **Entity:** `Notification`, `EmailLog`
- **VO:** `NotificationType`, `NotificationStatus`, `EmailStatus`
- **Port:** `NotificationRepository`, `EmailLogRepository`

### DDL — Flyway `V1__init.sql`

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID,                                  -- ref users.id, no cross-service FK
    order_id UUID,                                           -- ref orders.id, no cross-service FK
    type VARCHAR(30) NOT NULL,                               -- ORDER_CONFIRMED|ORDER_CANCELLED|ORDER_SHIPPED|RESET_PASSWORD
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',            -- UNREAD|READ|ARCHIVED
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',          -- LOW|NORMAL|HIGH
    read_at TIMESTAMP,
    data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE email_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID REFERENCES notifications(id),
    order_id UUID,                                           -- ref orders.id, no cross-service FK
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_key VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',           -- PENDING|SENT|FAILED
    provider VARCHAR(50),
    provider_message_id VARCHAR(100),
    sent_at TIMESTAMP,
    error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(recipient_user_id, status);
CREATE INDEX idx_notifications_order ON notifications(order_id);
CREATE INDEX idx_notifications_type ON notifications(type, status);
CREATE INDEX idx_email_logs_notification ON email_logs(notification_id);
CREATE INDEX idx_email_logs_order ON email_logs(order_id);
CREATE INDEX idx_email_logs_status ON email_logs(status, created_at DESC);
```

---

## 6. Cross-Service References (không FK liên DB)

| Tham chiếu | Nguồn | Đích | Cách xử lý |
|---|---|---|---|
| `cart_items.product_id` / `order_items.product_id` | order_db | products.id (product_db) | Snapshot name/image/price khi đặt hàng |
| `orders.user_id` | order_db | users.id | Gọi user-service API khi cần info user |
| `orders.shipping_address` | order_db | — | Snapshot JSONB (BR05), không ref `addresses` |
| `voucher_usages.order_id` | user_db | orders.id | Reference only |
| `payments.order_id` | pay_db | orders.id | Đồng bộ qua event MQ |
| `payment_transactions.order_id` | pay_db | orders.id | Lưu attempt/callback/refund theo payment |
| `notifications.order_id` / `email_logs.order_id` | notif_db | orders.id | Nhận từ event `order.confirmed/cancelled` |
| `email_logs.notification_id` | notif_db | notifications.id | FK nội bộ notification-service |

---

## 7. Migration & Audit Strategy

| Service | DB | Migration | Audit |
|---|---|---|---|
| user-service | PostgreSQL | Flyway `V*.sql` | `BaseEntity` (JPA Auditing) |
| product-service | PostgreSQL | Flyway `V*.sql` | `BaseEntity` |
| order-service | PostgreSQL | Flyway `V*.sql` | `BaseEntity` |
| payment-service | PostgreSQL | Flyway `V*.sql` | `BaseEntity` |
| notification-service | PostgreSQL | Flyway `V*.sql` | `BaseEntity` |

> **Lưu ý:** Bật JPA Auditing bằng `@EnableJpaAuditing` + `AuditorAware<String>` (lấy `X-User-Id` từ gateway). Repo đang để `ddl-auto: update` ở dev — prod chuyển sang Flyway và `ddl-auto: validate`.

### Migration notes
- `product-service`: chuyển từ MongoDB sang PostgreSQL cần migration dữ liệu `products/categories` sang `products/categories/product_variants/product_images/vendors`; `order-service` chỉ lưu UUID product + snapshot nên không FK liên DB.
- `user-service`: `users.password_hash` cho phép `NULL` để hỗ trợ OAuth-only user; thêm unique constraint cho `oauth_accounts(provider, provider_user_id)` và `oauth_accounts(user_id, provider)`.
- `user-service`: `vouchers.min_account_age_days` — số ngày tính từ `users.created_at` (UTC, truncate theo ngày lịch); user chỉ áp được voucher khi `accountAgeDays >= min_account_age_days`. Flyway `V2__vouchers_min_account_age_days.sql` nếu đã deploy V1.
- `notification-service`: thay `notification_logs` bằng `notifications` + `email_logs`; email reset password có thể chỉ ghi `email_logs` mà không cần `notification_id`.
- `payment-service`: thêm `payment_transactions` để lưu từng attempt/callback/refund, còn `payments.status` là trạng thái tổng hợp mới nhất.

---

## 8. Seed Data (dev)

| Bảng | Seed |
|---|---|
| users | 1 ADMIN, 1 STAFF, 2 CUSTOMER (password bcrypt) |
| categories | Áo, Quần, Phụ kiện (+ sub-category) |
| vendors | Basic Collection, Local Supplier, Fashion Brand |
| products | ~10 sản phẩm, mỗi cái 2–4 variant có stock |
| vouchers | `SUMMER10` (10%, `max_discount_amount=150000`, `audience_type=ALL_USERS`), `WELCOME50K` (fixed, `first_order_only=true`, `audience_type=NEW_CUSTOMER`), `LOYAL30` (10%, `max_discount_amount=300000`, `min_account_age_days=30`, `audience_type=LOYAL_CUSTOMER`) |
| voucher_allowed_users | 1–2 dòng mẫu cho voucher `SPECIFIC_USERS` nếu cần test audience riêng |
| notifications | 2–3 thông báo order mẫu cho CUSTOMER |
| email_logs | 2–3 email order/reset-password mẫu |

---

*Cartzilla Database Design v2.2 — Database-per-Service · PostgreSQL product DB · OAuth · payment transactions · split notifications/email · voucher min account age · aligned với repo SE1911-JV_MSS301*
