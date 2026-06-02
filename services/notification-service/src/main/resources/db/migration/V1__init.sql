CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID,
    recipient_email VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subject VARCHAR(255),
    sent_at TIMESTAMP,
    error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false, deleted_at TIMESTAMP
);
CREATE INDEX idx_notif_order ON notification_logs(order_id);
CREATE INDEX idx_notif_type  ON notification_logs(type, status);
