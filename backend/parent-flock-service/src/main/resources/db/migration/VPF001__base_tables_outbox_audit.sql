CREATE SCHEMA IF NOT EXISTS pf;

COMMENT ON SCHEMA pf IS 'Schema for the parent-flock service: outbox events, processed events, and audit logging.';

CREATE TABLE pf.outbox_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         JSONB,
    correlation_id  VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed       BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE pf.processed_event (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pf.audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100),
    entity_id      VARCHAR(255),
    performed_by   VARCHAR(255),
    details        JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_event_aggregate_id ON pf.outbox_event(aggregate_id);
CREATE INDEX idx_outbox_event_processed ON pf.outbox_event(processed, created_at);
CREATE INDEX idx_outbox_event_created_at ON pf.outbox_event(created_at);
CREATE INDEX idx_audit_log_entity ON pf.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON pf.audit_log(created_at);

COMMENT ON TABLE pf.outbox_event IS 'Outbox table for reliable event publishing.';
COMMENT ON TABLE pf.processed_event IS 'Deduplication log for already handled events.';
COMMENT ON TABLE pf.audit_log IS 'Audit trail for parent-flock service actions.';
