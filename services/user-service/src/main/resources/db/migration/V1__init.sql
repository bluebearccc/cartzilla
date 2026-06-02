CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100),
    phone         VARCHAR(20),
    role          VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    is_active     BOOLEAN NOT NULL DEFAULT true,
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

CREATE TABLE vouchers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(50) NOT NULL UNIQUE,
    discount_type     VARCHAR(20) NOT NULL,
    discount_value    DECIMAL(10,2) NOT NULL,
    min_order_amount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    max_uses          INTEGER NOT NULL DEFAULT 1,
    used_count        INTEGER NOT NULL DEFAULT 0,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    expires_at        TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by        VARCHAR(255), updated_by VARCHAR(255),
    is_deleted        BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE voucher_usages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id  UUID NOT NULL REFERENCES vouchers(id),
    user_id     UUID NOT NULL,
    order_id    UUID NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by  VARCHAR(255), updated_by VARCHAR(255),
    is_deleted  BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_vouchers_code  ON vouchers(code);
