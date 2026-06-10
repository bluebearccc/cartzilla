CREATE TABLE payment_transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id        UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    order_id          UUID NOT NULL,
    transaction_type  VARCHAR(30) NOT NULL,
    provider          VARCHAR(20) NOT NULL,
    provider_txn_ref  VARCHAR(100),
    amount            DECIMAL(12,2) NOT NULL,
    currency          VARCHAR(3) NOT NULL DEFAULT 'VND',
    status            VARCHAR(20) NOT NULL,
    request_payload   JSONB,
    response_payload  JSONB,
    error_code        VARCHAR(50),
    error_message     TEXT,
    processed_at      TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    is_deleted        BOOLEAN NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP
);

CREATE UNIQUE INDEX uq_payment_txn_ref ON payment_transactions (provider, provider_txn_ref) WHERE provider_txn_ref IS NOT NULL;
