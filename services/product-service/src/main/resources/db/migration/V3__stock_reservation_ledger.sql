CREATE TABLE IF NOT EXISTS stock_reservations (
    order_id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_stock_reservation_status CHECK (status IN ('PENDING','RESERVED','FAILED','RELEASED'))
);
