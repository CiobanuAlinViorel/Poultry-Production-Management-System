-- Role.allowedScopeTypes is mapped as an @ElementCollection; replace the array column with a collection table.
CREATE TABLE IF NOT EXISTS admin.role_allowed_scope_types (
    role_id UUID NOT NULL REFERENCES admin.role(id) ON DELETE CASCADE,
    scope_type VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_id, scope_type)
);

INSERT INTO admin.role_allowed_scope_types (role_id, scope_type)
SELECT r.id, s.scope_type
FROM admin.role r, unnest(r.allowed_scope_types) AS s(scope_type)
ON CONFLICT DO NOTHING;

ALTER TABLE admin.role DROP COLUMN IF EXISTS allowed_scope_types;
