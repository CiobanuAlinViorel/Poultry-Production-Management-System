-- Create admin user alin.ciobanu@ppms.ro with SYSTEM_ADMIN global role
-- Run inside psql connected to the PPMS Postgres container

\c ppms_identity

WITH new_user AS (
    INSERT INTO identity.users (id, email, username, status, failed_login_attempts, created_at, updated_at)
    VALUES (
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'alin.ciobanu@ppms.ro',
        'alin.ciobanu',
        'ACTIVE',
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT (email) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = NOW()
    RETURNING id
)
INSERT INTO identity.credentials (user_id, password_hash, password_updated_at, must_change_password, mfa_enabled, created_at, updated_at)
SELECT id,
       '$2a$10$JZZjUkb8zIykrVj9fSuEHuV3GKSs07T/5wNV5kOBfho8Bpnz1fhPK',
       NOW(),
       false,
       false,
       NOW(),
       NOW()
FROM new_user
ON CONFLICT (user_id) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    password_updated_at = NOW(),
    updated_at = NOW();

\c ppms_admin

INSERT INTO admin.user_role_assignment (id, user_id, role_id, scope_type, scope_id, assigned_by, assigned_at, valid_from, valid_to, revoked_at, revoked_by, reason, created_at, updated_at, version)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567891',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    '00000000-0000-0000-0000-000000000001',
    'GLOBAL',
    NULL,
    '00000000-0000-0000-0000-000000000000',
    NOW(),
    NOW(),
    NULL,
    NULL,
    NULL,
    'Manual admin bootstrap',
    NOW(),
    NOW(),
    0
)
ON CONFLICT (id) DO NOTHING;
