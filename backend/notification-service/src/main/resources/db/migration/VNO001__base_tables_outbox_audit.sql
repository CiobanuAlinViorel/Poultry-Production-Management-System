CREATE SCHEMA IF NOT EXISTS notification;

COMMENT ON SCHEMA notification IS 'Schema for the notification service: outbox events, processed events, and audit logging.';

CREATE TABLE notification.outbox_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         JSONB,
    correlation_id  VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed       BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE notification.processed_event (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification.audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100),
    entity_id      VARCHAR(255),
    performed_by   VARCHAR(255),
    details        JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_event_aggregate_id ON notification.outbox_event(aggregate_id);
CREATE INDEX idx_outbox_event_processed ON notification.outbox_event(processed, created_at);
CREATE INDEX idx_outbox_event_created_at ON notification.outbox_event(created_at);
CREATE INDEX idx_audit_log_entity ON notification.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON notification.audit_log(created_at);

COMMENT ON TABLE notification.outbox_event IS 'Outbox table for reliable event publishing.';
COMMENT ON TABLE notification.processed_event IS 'Deduplication log for already handled events.';
COMMENT ON TABLE notification.audit_log IS 'Audit trail for notification service actions.';
