ALTER TABLE identity.refresh_token
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64),
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS identity.kiosk_device (
    id UUID PRIMARY KEY,
    site_id UUID NOT NULL,
    name VARCHAR(255),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID,
    updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_kiosk_site ON identity.kiosk_device (site_id);

CREATE TABLE IF NOT EXISTS identity.user_pin (
    user_id UUID PRIMARY KEY REFERENCES identity.users(id),
    pin_hash VARCHAR(255) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS identity.api_client (
    id UUID PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    allowed_scopes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
