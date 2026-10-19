INSERT INTO admin.role (id, code, name_ro, name_en, description, module, is_system_defined, active, created_at, updated_at, version)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN', 'Administrator sistem', 'System Administrator', 'Full system access', 'SYSTEM', true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER', 'Manager', 'Manager', 'Read-only manager', 'SYSTEM', true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000003', 'HR_ADMIN', 'HR Administrator', 'HR Administrator', 'HR and workforce', 'SYSTEM', true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000004', 'FLOCK_SUPERVISOR', 'Supervizor efectiv reproducție', 'Flock Supervisor', 'Parent flock site supervisor', 'PARENT_FLOCK', true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000005', 'FARM_MANAGER', 'Manager fermă', 'Farm Manager', 'Farm manager', 'FARM', true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000006', 'HATCHERY_SUPERVISOR', 'Supervizor stație incubație', 'Hatchery Supervisor', 'Hatchery supervisor', 'HATCHERY', true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000007', 'SLAUGHTER_MANAGER', 'Manager abator', 'Slaughterhouse Manager', 'Slaughterhouse manager', 'SLAUGHTERHOUSE', true, true, NOW(), NOW(), 0)
ON CONFLICT (code) DO UPDATE SET
    name_ro = EXCLUDED.name_ro,
    name_en = EXCLUDED.name_en,
    description = EXCLUDED.description,
    module = EXCLUDED.module,
    updated_at = NOW();

INSERT INTO admin.role_allowed_scope_types (role_id, scope_type)
SELECT r.id, v.scope_type
FROM (VALUES
    ('SYSTEM_ADMIN', 'GLOBAL'),
    ('MANAGER', 'GLOBAL'),
    ('HR_ADMIN', 'GLOBAL'),
    ('FLOCK_SUPERVISOR', 'BREEDER_SITE'),
    ('FARM_MANAGER', 'FARM'),
    ('HATCHERY_SUPERVISOR', 'HATCHERY'),
    ('SLAUGHTER_MANAGER', 'SLAUGHTERHOUSE')
) AS v(code, scope_type)
JOIN admin.role r ON r.code = v.code
ON CONFLICT DO NOTHING;

INSERT INTO admin.permission (id, key, module, description, is_system_defined, created_at, updated_at, version)
VALUES
    ('00000000-0000-0000-0000-000000001001', 'admin:role:write', 'ADMIN', 'Manage roles', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000001002', 'admin:assignment:write', 'ADMIN', 'Manage role assignments', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000001003', 'admin:employee:write', 'ADMIN', 'Manage employees', true, NOW(), NOW(), 0)
ON CONFLICT (key) DO UPDATE SET
    description = EXCLUDED.description,
    updated_at = NOW();

-- Bootstrap admin assignment
INSERT INTO admin.user_role_assignment (id, user_id, role_id, scope_type, scope_id, assigned_by, assigned_at, valid_from, valid_to, revoked_at, revoked_by, reason, created_at, updated_at, version)
SELECT '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000000', id, 'GLOBAL', NULL, '00000000-0000-0000-0000-000000000000', NOW(), NOW(), NULL, NULL, NULL, 'Bootstrap admin', NOW(), NOW(), 0
FROM admin.role WHERE code = 'SYSTEM_ADMIN'
ON CONFLICT (id) DO NOTHING;
