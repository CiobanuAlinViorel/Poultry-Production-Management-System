CREATE TABLE IF NOT EXISTS hatchery.hatchery (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255),
    address TEXT,
    county VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_set_capacity INT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hatchery.incubator (
    id UUID PRIMARY KEY,
    hatchery_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    type VARCHAR(20),
    manufacturer VARCHAR(128),
    model VARCHAR(128),
    capacity_eggs INT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hatchery.egg_batch (
    id UUID PRIMARY KEY,
    batch_code VARCHAR(64) NOT NULL UNIQUE,
    hatchery_id UUID NOT NULL,
    origin_flock_id UUID,
    origin_site_id UUID,
    breed_code VARCHAR(32),
    collection_date TIMESTAMPTZ,
    qty_accepted INT,
    qty_available INT,
    qty_set INT,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_STORE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hatchery.incubation_batch (
    id UUID PRIMARY KEY,
    batch_code VARCHAR(64) NOT NULL UNIQUE,
    hatchery_id UUID NOT NULL,
    setter_id UUID,
    hatcher_id UUID,
    set_at TIMESTAMPTZ,
    expected_hatch_date TIMESTAMPTZ,
    qty_set INT,
    status VARCHAR(20) NOT NULL DEFAULT 'SET',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hatchery.chick_batch (
    id UUID PRIMARY KEY,
    batch_code VARCHAR(64) NOT NULL UNIQUE,
    hatchery_id UUID NOT NULL,
    hatch_record_id UUID,
    hatch_date TIMESTAMPTZ,
    grade VARCHAR(8),
    qty_available INT,
    qty_boxed INT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_incubator_hatchery ON hatchery.incubator (hatchery_id);
CREATE INDEX IF NOT EXISTS idx_egg_batch_hatchery ON hatchery.egg_batch (hatchery_id);
CREATE INDEX IF NOT EXISTS idx_incubation_batch_hatchery ON hatchery.incubation_batch (hatchery_id);
CREATE INDEX IF NOT EXISTS idx_chick_batch_hatchery ON hatchery.chick_batch (hatchery_id);
