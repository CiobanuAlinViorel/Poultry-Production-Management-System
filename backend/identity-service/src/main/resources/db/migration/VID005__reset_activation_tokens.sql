CREATE TABLE IF NOT EXISTS identity.password_reset_token (
    token_hash VARCHAR(255) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    requested_ip VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_user ON identity.password_reset_token (user_id);

CREATE TABLE IF NOT EXISTS identity.activation_token (
    token_hash VARCHAR(255) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_activation_user ON identity.activation_token (user_id);
