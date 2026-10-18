CREATE SCHEMA IF NOT EXISTS sl;

COMMENT ON SCHEMA sl IS 'Schema for the slaughterhouse service: outbox events, processed events, and audit logging.';

CREATE TABLE sl.outbox_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         JSONB,
    correlation_id  VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed       BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE sl.processed_event (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sl.audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100),
    entity_id      VARCHAR(255),
    performed_by   VARCHAR(255),
    details        JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_event_aggregate_id ON sl.outbox_event(aggregate_id);
CREATE INDEX idx_outbox_event_processed ON sl.outbox_event(processed, created_at);
CREATE INDEX idx_outbox_event_created_at ON sl.outbox_event(created_at);
CREATE INDEX idx_audit_log_entity ON sl.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON sl.audit_log(created_at);

COMMENT ON TABLE sl.outbox_event IS 'Outbox table for reliable event publishing.';
COMMENT ON TABLE sl.processed_event IS 'Deduplication log for already handled events.';
COMMENT ON TABLE sl.audit_log IS 'Audit trail for slaughterhouse service actions.';
