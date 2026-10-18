CREATE TABLE pf.breeder_site (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500),
    county          VARCHAR(100),
    active          BOOLEAN NOT NULL DEFAULT true,
    capacity_houses INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE pf.house (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id         UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    capacity        INTEGER NOT NULL DEFAULT 0,
    area_m2         NUMERIC(10, 2),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE pf.flock (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id             UUID NOT NULL,
    house_id            UUID NOT NULL,
    breed_code          VARCHAR(50) NOT NULL,
    arrival_date        DATE NOT NULL,
    expected_end_date     DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    female_count        INTEGER NOT NULL DEFAULT 0,
    male_count          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_flock_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE pf.daily_production_sheet (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flock_id            UUID NOT NULL,
    sheet_date          DATE NOT NULL,
    eggs_total          INTEGER NOT NULL DEFAULT 0,
    eggs_cracked        INTEGER NOT NULL DEFAULT 0,
    eggs_dirty          INTEGER NOT NULL DEFAULT 0,
    eggs_small          INTEGER NOT NULL DEFAULT 0,
    eggs_double_yolk    INTEGER NOT NULL DEFAULT 0,
    feed_kg             NUMERIC(10, 3),
    water_liters        NUMERIC(10, 2),
    mortality           INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_eggs_total_non_negative CHECK (eggs_total >= 0),
    CONSTRAINT chk_eggs_cracked_non_negative CHECK (eggs_cracked >= 0),
    CONSTRAINT chk_eggs_dirty_non_negative CHECK (eggs_dirty >= 0),
    CONSTRAINT chk_eggs_small_non_negative CHECK (eggs_small >= 0),
    CONSTRAINT chk_eggs_double_yolk_non_negative CHECK (eggs_double_yolk >= 0),
    CONSTRAINT chk_mortality_non_negative CHECK (mortality >= 0)
);

CREATE TABLE pf.egg_batch (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flock_id        UUID NOT NULL,
    batch_code      VARCHAR(100) NOT NULL UNIQUE,
    laid_date       DATE NOT NULL,
    storage_days    INTEGER NOT NULL DEFAULT 0,
    quality_grade   VARCHAR(20),
    quantity        INTEGER NOT NULL DEFAULT 0,
    reserved_for    VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'IN_STORE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_egg_batch_status CHECK (status IN ('IN_STORE', 'RESERVED', 'DISPATCHED', 'DISCARDED')),
    CONSTRAINT chk_egg_batch_quantity_non_negative CHECK (quantity >= 0)
);

CREATE TABLE pf.egg_transfer_order (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_site_id        UUID NOT NULL,
    to_hatchery_id      UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    transfer_date       DATE NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_egg_transfer_status CHECK (status IN ('PLANNED', 'APPROVED', 'DISPATCHED', 'RECEIVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_house_site_id ON pf.house(site_id);
CREATE INDEX idx_house_active ON pf.house(active);
CREATE UNIQUE INDEX idx_house_code_per_site ON pf.house(site_id, code);

CREATE INDEX idx_flock_site_id ON pf.flock(site_id);
CREATE INDEX idx_flock_house_id ON pf.flock(house_id);
CREATE INDEX idx_flock_status ON pf.flock(status);
CREATE INDEX idx_flock_arrival_date ON pf.flock(arrival_date);

CREATE INDEX idx_daily_production_sheet_flock_id ON pf.daily_production_sheet(flock_id);
CREATE UNIQUE INDEX idx_daily_production_sheet_flock_date ON pf.daily_production_sheet(flock_id, sheet_date);
CREATE INDEX idx_daily_production_sheet_date ON pf.daily_production_sheet(sheet_date);

CREATE INDEX idx_egg_batch_flock_id ON pf.egg_batch(flock_id);
CREATE INDEX idx_egg_batch_laid_date ON pf.egg_batch(laid_date);
CREATE INDEX idx_egg_batch_status ON pf.egg_batch(status);

CREATE INDEX idx_egg_transfer_from_site ON pf.egg_transfer_order(from_site_id);
CREATE INDEX idx_egg_transfer_to_hatchery ON pf.egg_transfer_order(to_hatchery_id);
CREATE INDEX idx_egg_transfer_status ON pf.egg_transfer_order(status);

COMMENT ON TABLE pf.breeder_site IS 'Breeder farm sites owned by the parent-flock service.';
COMMENT ON TABLE pf.house IS 'Houses (rearing or production) inside a breeder site.';
COMMENT ON TABLE pf.flock IS 'Parent flocks placed in houses.';
COMMENT ON TABLE pf.daily_production_sheet IS 'Daily production record per flock.';
COMMENT ON TABLE pf.egg_batch IS 'Batch of eggs collected from a flock on a given date.';
COMMENT ON TABLE pf.egg_transfer_order IS 'Order to dispatch eggs from a breeder site to a hatchery.';
