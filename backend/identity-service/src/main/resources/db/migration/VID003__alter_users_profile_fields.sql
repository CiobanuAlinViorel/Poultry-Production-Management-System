ALTER TABLE identity.users
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS locale VARCHAR(8) NOT NULL DEFAULT 'ro',
    ADD COLUMN IF NOT EXISTS employee_id UUID,
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON identity.users (email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON identity.users (username);
CREATE INDEX IF NOT EXISTS idx_users_employee ON identity.users (employee_id);

UPDATE identity.users SET locale = 'ro' WHERE locale IS NULL;
