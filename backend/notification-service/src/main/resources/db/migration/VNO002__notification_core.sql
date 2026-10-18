CREATE TABLE IF NOT EXISTS notification.alert (
    id UUID PRIMARY KEY,
    alert_code VARCHAR(64) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    scope_type VARCHAR(64),
    scope_id UUID,
    site_name VARCHAR(255),
    entity_type VARCHAR(64),
    entity_id UUID,
    params JSONB,
    title_key VARCHAR(128),
    message_key VARCHAR(128),
    dedup_key VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    raised_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ,
    occurrences INT NOT NULL DEFAULT 1,
    acknowledged_by UUID,
    acknowledged_at TIMESTAMPTZ,
    ack_reason TEXT,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notification.notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    alert_id UUID,
    type VARCHAR(20) NOT NULL,
    title_key VARCHAR(128),
    body_key VARCHAR(128),
    params JSONB,
    link VARCHAR(500),
    channel_results JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notification.user_preference (
    user_id UUID PRIMARY KEY,
    locale VARCHAR(8) NOT NULL DEFAULT 'ro',
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_hours VARCHAR(64),
    digest VARCHAR(20) NOT NULL DEFAULT 'NONE',
    muted_alert_codes JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notification.email_outbox (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body_html TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_alert_status ON notification.alert (status);
CREATE INDEX IF NOT EXISTS idx_alert_dedup ON notification.alert (dedup_key);
CREATE INDEX IF NOT EXISTS idx_alert_scope ON notification.alert (scope_type, scope_id);
CREATE INDEX IF NOT EXISTS idx_notification_user ON notification.notification (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_read ON notification.notification (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_email_status ON notification.email_outbox (status);
