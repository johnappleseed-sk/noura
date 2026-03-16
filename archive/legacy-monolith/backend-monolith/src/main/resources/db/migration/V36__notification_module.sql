CREATE TABLE IF NOT EXISTS notification_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    related_entity_type VARCHAR(80),
    related_entity_id UUID,
    sent_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_notification_messages_user
        FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notification_messages_recipient_created
    ON notification_messages (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_messages_status_created
    ON notification_messages (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_messages_type_created
    ON notification_messages (type, created_at DESC);
