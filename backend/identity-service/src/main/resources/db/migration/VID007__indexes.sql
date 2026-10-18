CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON identity.refresh_token (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_family_id ON identity.refresh_token (family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON identity.refresh_token (expires_at);
CREATE INDEX IF NOT EXISTS idx_login_audit_event_user_id ON identity.login_audit_event (user_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_event_occurred ON identity.login_audit_event (occurred_at);
