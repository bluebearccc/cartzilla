CREATE TABLE oauth_accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id),
    provider          VARCHAR(30) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    provider_email    VARCHAR(255),
    display_name      VARCHAR(100),
    avatar_url        TEXT,
    linked_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at     TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    is_deleted        BOOLEAN NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP,
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_oauth_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE voucher_allowed_users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id        UUID NOT NULL REFERENCES vouchers(id),
    user_id           UUID NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    is_deleted        BOOLEAN NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP,
    CONSTRAINT uq_voucher_allowed_user UNIQUE (voucher_id, user_id)
);
