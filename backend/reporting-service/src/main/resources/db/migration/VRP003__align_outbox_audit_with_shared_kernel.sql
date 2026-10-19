-- Align outbox/processed_event/audit_log with the shared-kernel entities
-- (OutboxEntity, ProcessedEventEntity, AuditLogEntity). The previous layout was never
-- usable by the application (schema validation failed on startup), so these tables are empty.
DROP TABLE IF EXISTS reporting.outbox_event;
DROP TABLE IF EXISTS reporting.processed_event;
DROP TABLE IF EXISTS reporting.audit_log;

CREATE TABLE reporting.outbox_event (
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

CREATE TABLE reporting.processed_event (
    event_id UUID NOT NULL,
    consumer VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, consumer)
);

CREATE TABLE reporting.audit_log (
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

CREATE INDEX idx_outbox_published ON reporting.outbox_event (published_at);
CREATE INDEX idx_outbox_created ON reporting.outbox_event (created_at);
CREATE INDEX idx_audit_occurred ON reporting.audit_log (occurred_at);
CREATE INDEX idx_audit_entity ON reporting.audit_log (entity_type, entity_id);
