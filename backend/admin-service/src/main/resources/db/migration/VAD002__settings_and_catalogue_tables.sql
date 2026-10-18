CREATE TABLE IF NOT EXISTS admin.setting_definition (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    module VARCHAR(64),
    setting_type VARCHAR(20) NOT NULL,
    unit VARCHAR(64),
    default_value VARCHAR(512),
    min_value VARCHAR(64),
    max_value VARCHAR(64),
    allowed_values TEXT[],
    description_ro TEXT,
    description_en TEXT,
    scopeable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admin.setting_value (
    id UUID PRIMARY KEY,
    definition_key VARCHAR(255) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    scope_id UUID,
    value VARCHAR(1024) NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_setting_value_lookup
    ON admin.setting_value (definition_key, scope_type, scope_id, effective_from);

CREATE TABLE IF NOT EXISTS admin.breed (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255),
    kind VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admin.unit_of_measure (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255),
    kind VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);
