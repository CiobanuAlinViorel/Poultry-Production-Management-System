ALTER TABLE identity.credentials
    ADD COLUMN IF NOT EXISTS password_history JSONB DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS mfa_secret_enc TEXT,
    ADD COLUMN IF NOT EXISTS mfa_recovery_codes JSONB DEFAULT '[]';
