INSERT INTO admin.role (id, code, name_ro, name_en, description, module, allowed_scope_types, is_system_defined, active, created_at, updated_at, version)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN', 'Administrator sistem', 'System Administrator', 'Full system access', 'SYSTEM', ARRAY['GLOBAL'], true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER', 'Manager', 'Manager', 'Read-only manager', 'SYSTEM', ARRAY['GLOBAL'], true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000003', 'HR_ADMIN', 'HR Administrator', 'HR Administrator', 'HR and workforce', 'SYSTEM', ARRAY['GLOBAL'], true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000004', 'FLOCK_SUPERVISOR', 'Supervizor efectiv reproducție', 'Flock Supervisor', 'Parent flock site supervisor', 'PARENT_FLOCK', ARRAY['BREEDER_SITE'], true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000005', 'FARM_MANAGER', 'Manager fermă', 'Farm Manager', 'Farm manager', 'FARM', ARRAY['FARM'], true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000006', 'HATCHERY_SUPERVISOR', 'Supervizor stație incubație', 'Hatchery Supervisor', 'Hatchery supervisor', 'HATCHERY', ARRAY['HATCHERY'], true, true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000007', 'SLAUGHTER_MANAGER', 'Manager abator', 'Slaughterhouse Manager', 'Slaughterhouse manager', 'SLAUGHTERHOUSE', ARRAY['SLAUGHTERHOUSE'], true, true, NOW(), NOW(), 0)
ON CONFLICT (code) DO UPDATE SET
    name_ro = EXCLUDED.name_ro,
    name_en = EXCLUDED.name_en,
    description = EXCLUDED.description,
    module = EXCLUDED.module,
    allowed_scope_types = EXCLUDED.allowed_scope_types,
    updated_at = NOW();

INSERT INTO admin.permission (id, key, module, description, is_system_defined, created_at, updated_at, version)
VALUES
    ('00000000-0000-0000-0000-000000001001', 'admin:role:write', 'ADMIN', 'Manage roles', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000001002', 'admin:assignment:write', 'ADMIN', 'Manage role assignments', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000001003', 'admin:employee:write', 'ADMIN', 'Manage employees', true, NOW(), NOW(), 0)
ON CONFLICT (key) DO UPDATE SET
    description = EXCLUDED.description,
    updated_at = NOW();
