INSERT INTO admin.setting_definition (
    id, setting_key, module, setting_type, unit, default_value, min_value, max_value, allowed_values,
    description_ro, description_en, scopeable, created_at, updated_at, version
)
VALUES
    -- Identity / security
    ('00000000-0000-0000-0000-000000010001', 'identity.password.minLength', 'IDENTITY', 'INT', NULL, '10', '6', '128', NULL,
     'Lungimea minimă a parolei', 'Minimum password length', false, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000010002', 'identity.lockout.threshold', 'IDENTITY', 'INT', NULL, '5', '1', '10', NULL,
     'Numărul maxim de autentificări eșuate înainte de blocare', 'Failed login attempts before lockout', false, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000010003', 'identity.lockout.autoUnlockMinutes', 'IDENTITY', 'INT', 'minutes', '30', '1', '1440', NULL,
     'Minute până la deblocarea automată a contului', 'Minutes until automatic account unlock', false, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000010004', 'identity.mfa.requiredRoles', 'IDENTITY', 'JSON', NULL, '["SYSTEM_ADMIN","HR_ADMIN"]', NULL, NULL, NULL,
     'Roluri pentru care este obligatorie MFA', 'Roles that require MFA', false, NOW(), NOW(), 0),

    -- Parent flock
    ('00000000-0000-0000-0000-000000020001', 'pf.flock.endOfLayWeeks', 'PARENT_FLOCK', 'INT', 'weeks', '64', '40', '80', NULL,
     'Săptămâna de sfârșit a perioadei de ouat', 'Week when flock reaches end of lay', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000020002', 'pf.flock.endOfLayWarningWeeks', 'PARENT_FLOCK', 'INT', 'weeks', '60', '40', '80', NULL,
     'Săptămâna de avertizare înainte de sfârșitul perioadei de ouat', 'End-of-lay warning week', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000020003', 'pf.egg.maxStorageDays', 'PARENT_FLOCK', 'INT', 'days', '7', '1', '30', NULL,
     'Numărul maxim de zile de stocare a ouălor', 'Maximum egg storage days', true, NOW(), NOW(), 0),

    -- Hatchery
    ('00000000-0000-0000-0000-000000030001', 'ha.setter.tempMin', 'HATCHERY', 'DECIMAL', '°C', '37.5', '35.0', '40.0', NULL,
     'Temperatura minimă în setter', 'Setter minimum temperature', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000030002', 'ha.setter.tempMax', 'HATCHERY', 'DECIMAL', '°C', '37.9', '35.0', '40.0', NULL,
     'Temperatura maximă în setter', 'Setter maximum temperature', true, NOW(), NOW(), 0),

    -- Farm
    ('00000000-0000-0000-0000-000000040001', 'fa.stocking.maxKgPerM2', 'FARM', 'DECIMAL', 'kg/m²', '33', '10', '60', NULL,
     'Densitatea maximă de populare', 'Maximum stocking density', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000040002', 'fa.downtime.minDays', 'FARM', 'INT', 'days', '10', '0', '90', NULL,
     'Zile minime de nefuncționare între loturi', 'Minimum downtime days between lots', true, NOW(), NOW(), 0),

    -- Slaughterhouse
    ('00000000-0000-0000-0000-000000050001', 'sl.lairage.minMinutes', 'SLAUGHTERHOUSE', 'INT', 'minutes', '60', '0', '1440', NULL,
     'Timp minim de așteptare în lairage', 'Minimum lairage time', true, NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000050002', 'sl.lairage.maxMinutes', 'SLAUGHTERHOUSE', 'INT', 'minutes', '720', '0', '1440', NULL,
     'Timp maxim de așteptare în lairage', 'Maximum lairage time', true, NOW(), NOW(), 0)
ON CONFLICT (setting_key) DO UPDATE SET
    module = EXCLUDED.module,
    setting_type = EXCLUDED.setting_type,
    unit = EXCLUDED.unit,
    default_value = EXCLUDED.default_value,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    allowed_values = EXCLUDED.allowed_values,
    description_ro = EXCLUDED.description_ro,
    description_en = EXCLUDED.description_en,
    scopeable = EXCLUDED.scopeable,
    updated_at = NOW();
