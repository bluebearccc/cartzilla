-- user-service schema (cartzilla_user_db) — khớp với JPA entities:
-- User, Address, RefreshToken, OAuthAccount, Voucher, VoucherUsage, VoucherAllowedUser

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255),                          -- nullable: user chỉ đăng nhập OAuth (UA-01)
    full_name      VARCHAR(100),
    phone          VARCHAR(20),
    role           VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',  -- CUSTOMER|STAFF|ADMIN
    email_verified BOOLEAN      NOT NULL DEFAULT false,
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by     VARCHAR(255), updated_by VARCHAR(255),
    is_deleted     BOOLEAN      NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE addresses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    street      VARCHAR(255) NOT NULL,
    district    VARCHAR(100) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN      NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token       TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN   NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE oauth_accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    provider         VARCHAR(30)  NOT NULL,                 -- GOOGLE|FACEBOOK
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email   VARCHAR(255),
    display_name     VARCHAR(100),
    avatar_url       TEXT,
    linked_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at    TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by       VARCHAR(255), updated_by VARCHAR(255),
    is_deleted       BOOLEAN   NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_oauth_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE vouchers (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                 VARCHAR(50)   NOT NULL,            -- unique theo upper(code)
    discount_type        VARCHAR(20)   NOT NULL,            -- PERCENTAGE|FIXED_AMOUNT
    discount_value       DECIMAL(10,2) NOT NULL,
    max_discount_amount  DECIMAL(12,2),                     -- bắt buộc > 0 với PERCENTAGE
    min_order_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_account_age_days INTEGER       NOT NULL DEFAULT 0 CHECK (min_account_age_days >= 0),
    per_user_limit       INTEGER       NOT NULL DEFAULT 1 CHECK (per_user_limit >= 1),
    audience_type        VARCHAR(30)   NOT NULL DEFAULT 'ALL_USERS', -- ALL_USERS|NEW_CUSTOMER|LOYAL_CUSTOMER|SPECIFIC_USERS
    first_order_only     BOOLEAN       NOT NULL DEFAULT false,
    min_completed_orders INTEGER       NOT NULL DEFAULT 0 CHECK (min_completed_orders >= 0),
    min_total_spent      DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (min_total_spent >= 0),
    max_uses             INTEGER       NOT NULL DEFAULT 1 CHECK (max_uses >= 1),
    used_count           INTEGER       NOT NULL DEFAULT 0,
    is_active            BOOLEAN       NOT NULL DEFAULT true,
    starts_at            TIMESTAMP,
    expires_at           TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by           VARCHAR(255), updated_by VARCHAR(255),
    is_deleted           BOOLEAN   NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    CONSTRAINT chk_vouchers_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_vouchers_discount_value CHECK (
        (discount_type = 'PERCENTAGE' AND discount_value > 0 AND discount_value <= 100 AND max_discount_amount IS NOT NULL AND max_discount_amount > 0)
        OR (discount_type = 'FIXED_AMOUNT' AND discount_value > 0)
    ),
    CONSTRAINT chk_vouchers_used_count CHECK (used_count >= 0 AND used_count <= max_uses),
    CONSTRAINT chk_vouchers_audience_type CHECK (audience_type IN ('ALL_USERS', 'NEW_CUSTOMER', 'LOYAL_CUSTOMER', 'SPECIFIC_USERS')),
    CONSTRAINT chk_vouchers_time_window CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at > starts_at)
);

CREATE TABLE voucher_usages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id      UUID NOT NULL REFERENCES vouchers(id),
    user_id         UUID NOT NULL,        -- ref users.id; không FK cross-service
    order_id        UUID NOT NULL,        -- ref orders.id; không FK cross-service
    discount_amount DECIMAL(12,2),        -- snapshot tiền giảm tại thời điểm redeem
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by      VARCHAR(255), updated_by VARCHAR(255),
    is_deleted      BOOLEAN   NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    CONSTRAINT uq_voucher_usage_order UNIQUE (voucher_id, order_id)  -- redeem idempotent
);

CREATE TABLE voucher_allowed_users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id  UUID NOT NULL REFERENCES vouchers(id),  -- audience_type = SPECIFIC_USERS
    user_id     UUID NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN   NOT NULL DEFAULT false, deleted_at TIMESTAMP,
    CONSTRAINT uq_voucher_allowed_user UNIQUE (voucher_id, user_id)
);

CREATE INDEX idx_users_email             ON users(email);
CREATE INDEX idx_addresses_user          ON addresses(user_id);
CREATE INDEX idx_refresh_tokens_tok      ON refresh_tokens(token);
CREATE INDEX idx_oauth_user              ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_provider          ON oauth_accounts(provider, provider_user_id);
CREATE UNIQUE INDEX uq_vouchers_code_upper ON vouchers(upper(code));
CREATE INDEX idx_voucher_usages_v        ON voucher_usages(voucher_id);
CREATE INDEX idx_voucher_usages_user     ON voucher_usages(voucher_id, user_id);
CREATE INDEX idx_voucher_allowed_users_v ON voucher_allowed_users(voucher_id);
