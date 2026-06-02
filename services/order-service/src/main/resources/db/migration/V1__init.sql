CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    product_id VARCHAR(50) NOT NULL,
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
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(12,2) NOT NULL,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    voucher_code VARCHAR(50),
    shipping_address JSONB NOT NULL,
    note TEXT, cancelled_reason TEXT, confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    product_id VARCHAR(50) NOT NULL,
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
CREATE INDEX idx_order_items_o  ON order_items(order_id);
CREATE INDEX idx_saga_order     ON saga_states(order_id);
