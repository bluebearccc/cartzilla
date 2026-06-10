-- Drop old combined table; replace with Notification + EmailLog split
DROP TABLE IF EXISTS notification_logs;

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID,
    order_id            UUID,
    type                VARCHAR(30)   NOT NULL,
    title               VARCHAR(255)  NOT NULL,
    message             TEXT          NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'UNREAD',
    priority            VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    read_at             TIMESTAMPTZ,
    data                JSONB,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    is_deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_notif_user    ON notifications(recipient_user_id);
CREATE INDEX idx_notif_order   ON notifications(order_id);
CREATE INDEX idx_notif_status  ON notifications(status, is_deleted);

CREATE TABLE email_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id     UUID          REFERENCES notifications(id),
    order_id            UUID,
    recipient_email     VARCHAR(255)  NOT NULL,
    subject             VARCHAR(255)  NOT NULL,
    template_key        VARCHAR(100),
    provider            VARCHAR(50),
    provider_message_id VARCHAR(100),
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    sent_at             TIMESTAMPTZ,
    error               TEXT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    is_deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_email_notif   ON email_logs(notification_id);
CREATE INDEX idx_email_order   ON email_logs(order_id);
CREATE INDEX idx_email_status  ON email_logs(status);
