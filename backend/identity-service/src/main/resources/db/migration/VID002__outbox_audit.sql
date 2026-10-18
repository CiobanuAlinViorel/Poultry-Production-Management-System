CREATE TABLE IF NOT EXISTS identity.outbox_event (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    key VARCHAR(255),
    headers JSONB,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE TABLE IF NOT EXISTS identity.processed_event (
    event_id UUID NOT NULL,
    consumer VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, consumer)
);

CREATE TABLE IF NOT EXISTS identity.audit_log (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_name VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255),
    entity_id UUID,
    site_id UUID,
    ip_address VARCHAR(255),
    user_agent TEXT,
    request_payload JSONB,
    outcome VARCHAR(50) NOT NULL,
    error_code VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_published ON identity.outbox_event (published_at);
CREATE INDEX IF NOT EXISTS idx_outbox_created ON identity.outbox_event (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_occurred ON identity.audit_log (occurred_at);
