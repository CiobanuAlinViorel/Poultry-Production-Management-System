CREATE SCHEMA IF NOT EXISTS admin;
COMMENT ON SCHEMA admin IS 'Authorisation, workforce and configuration context';

CREATE TABLE IF NOT EXISTS admin.outbox_event (
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

CREATE TABLE IF NOT EXISTS admin.processed_event (
    event_id UUID NOT NULL,
    consumer VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, consumer)
);

CREATE TABLE IF NOT EXISTS admin.audit_log (
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

CREATE TABLE IF NOT EXISTS admin.role (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name_ro VARCHAR(255),
    name_en VARCHAR(255),
    description TEXT,
    module VARCHAR(64),
    allowed_scope_types VARCHAR(64)[] DEFAULT '{}',
    is_system_defined BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admin.permission (
    id UUID PRIMARY KEY,
    key VARCHAR(128) NOT NULL UNIQUE,
    module VARCHAR(64),
    description TEXT,
    is_system_defined BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admin.role_permission (
    role_id UUID NOT NULL REFERENCES admin.role(id),
    permission_id UUID NOT NULL REFERENCES admin.permission(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS admin.user_role_assignment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL REFERENCES admin.role(id),
    scope_type VARCHAR(64) NOT NULL,
    scope_id UUID,
    assigned_by UUID,
    assigned_at TIMESTAMPTZ NOT NULL,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by UUID,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admin.site_directory_entry (
    site_id UUID PRIMARY KEY,
    site_type VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255),
    address TEXT,
    county VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source_service VARCHAR(64),
    last_event_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_outbox_published ON admin.outbox_event (published_at);
CREATE INDEX IF NOT EXISTS idx_outbox_created ON admin.outbox_event (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_occurred ON admin.audit_log (occurred_at);
CREATE INDEX IF NOT EXISTS idx_assignment_user ON admin.user_role_assignment (user_id);
CREATE INDEX IF NOT EXISTS idx_assignment_scope ON admin.user_role_assignment (scope_type, scope_id);
