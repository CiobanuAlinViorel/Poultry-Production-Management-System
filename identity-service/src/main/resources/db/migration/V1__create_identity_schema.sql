CREATE TABLE identity.users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) NOT NULL UNIQUE,
    username               VARCHAR(255) UNIQUE,
    status                 VARCHAR(20) NOT NULL,
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    last_login_at          TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE identity.credentials (
    user_id               UUID PRIMARY KEY REFERENCES identity.users(id),
    password_hash         VARCHAR(255) NOT NULL,
    password_updated_at   TIMESTAMPTZ,
    must_change_password  BOOLEAN NOT NULL DEFAULT false,
    mfa_enabled           BOOLEAN NOT NULL DEFAULT false,
    updated_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE identity.refresh_token (
    token_hash              VARCHAR(255) PRIMARY KEY,
    user_id                 UUID NOT NULL REFERENCES identity.users(id),
    family_id               UUID NOT NULL,
    issued_at               TIMESTAMPTZ NOT NULL,
    expires_at              TIMESTAMPTZ NOT NULL,
    revoked_at              TIMESTAMPTZ,
    replaced_by_token_hash  VARCHAR(255),
    device_info             VARCHAR(255)
);

CREATE TABLE identity.login_audit_event (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID REFERENCES identity.users(id),
    email               VARCHAR(255) NOT NULL,
    success             BOOLEAN NOT NULL,
    login_event_reason  VARCHAR(30),
    ip_address          VARCHAR(64) NOT NULL,
    occurred_at         TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_token_user_id ON identity.refresh_token(user_id);
CREATE INDEX idx_refresh_token_family_id ON identity.refresh_token(family_id);
CREATE INDEX idx_login_audit_event_user_id ON identity.login_audit_event(user_id);
