CREATE SCHEMA IF NOT EXISTS fa;

COMMENT ON SCHEMA fa IS 'Schema for the farm service: outbox events, processed events, and audit logging.';

CREATE TABLE fa.outbox_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         JSONB,
    correlation_id  VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed       BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE fa.processed_event (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE fa.audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100),
    entity_id      VARCHAR(255),
    performed_by   VARCHAR(255),
    details        JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_event_aggregate_id ON fa.outbox_event(aggregate_id);
CREATE INDEX idx_outbox_event_processed ON fa.outbox_event(processed, created_at);
CREATE INDEX idx_outbox_event_created_at ON fa.outbox_event(created_at);
CREATE INDEX idx_audit_log_entity ON fa.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON fa.audit_log(created_at);

COMMENT ON TABLE fa.outbox_event IS 'Outbox table for reliable event publishing.';
COMMENT ON TABLE fa.processed_event IS 'Deduplication log for already handled events.';
COMMENT ON TABLE fa.audit_log IS 'Audit trail for farm service actions.';
