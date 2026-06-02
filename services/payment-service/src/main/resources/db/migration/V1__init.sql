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
CREATE INDEX idx_payments_order ON payments(order_id);
