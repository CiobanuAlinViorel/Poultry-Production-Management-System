CREATE TABLE fa.farm (
    id UUID PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(255) NOT NULL,
    address VARCHAR(500), county VARCHAR(100), active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE fa.house (
    id UUID PRIMARY KEY, farm_id UUID NOT NULL REFERENCES fa.farm(id),
    code VARCHAR(50) NOT NULL, name VARCHAR(255) NOT NULL, capacity INTEGER NOT NULL,
    area_m2 NUMERIC(12,2) NOT NULL, active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_house_farm_code UNIQUE (farm_id, code),
    CONSTRAINT ck_house_capacity CHECK (capacity >= 0), CONSTRAINT ck_house_area CHECK (area_m2 > 0)
);

CREATE TABLE fa.silo (
    id UUID PRIMARY KEY, farm_id UUID NOT NULL REFERENCES fa.farm(id),
    code VARCHAR(50) NOT NULL, name VARCHAR(255) NOT NULL, capacity_kg NUMERIC(14,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_silo_farm_code UNIQUE (farm_id, code), CONSTRAINT ck_silo_capacity CHECK (capacity_kg > 0)
);

CREATE TABLE fa.lot (
    id UUID PRIMARY KEY, farm_id UUID NOT NULL REFERENCES fa.farm(id), house_id UUID NOT NULL REFERENCES fa.house(id),
    breed_code VARCHAR(100) NOT NULL, chick_batch_id UUID, arrival_date DATE NOT NULL,
    female_count INTEGER NOT NULL, male_count INTEGER NOT NULL, status VARCHAR(20) NOT NULL,
    expected_catch_date DATE,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_lot_counts CHECK (female_count >= 0 AND male_count >= 0),
    CONSTRAINT ck_lot_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);
CREATE UNIQUE INDEX uk_lot_active_house ON fa.lot(house_id) WHERE status = 'ACTIVE';

CREATE TABLE fa.daily_sheet (
    id UUID PRIMARY KEY, lot_id UUID NOT NULL REFERENCES fa.lot(id), date DATE NOT NULL,
    mortality INTEGER NOT NULL DEFAULT 0, feed_kg NUMERIC(14,3), water_liters NUMERIC(14,3),
    avg_weight_g NUMERIC(10,2), temperature_min NUMERIC(5,2), temperature_max NUMERIC(5,2),
    humidity_pct NUMERIC(5,2), comments TEXT,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_daily_sheet_lot_date UNIQUE (lot_id, date), CONSTRAINT ck_daily_mortality CHECK (mortality >= 0)
);

CREATE TABLE fa.farm_delivery (
    id UUID PRIMARY KEY, lot_id UUID NOT NULL REFERENCES fa.lot(id), dispatch_date DATE NOT NULL,
    quantity INTEGER NOT NULL, avg_weight_kg NUMERIC(10,3) NOT NULL, fci_ref VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_delivery_quantity CHECK (quantity > 0), CONSTRAINT ck_delivery_weight CHECK (avg_weight_kg > 0)
);

CREATE INDEX idx_house_farm ON fa.house(farm_id);
CREATE INDEX idx_silo_farm ON fa.silo(farm_id);
CREATE INDEX idx_lot_farm ON fa.lot(farm_id);
CREATE INDEX idx_lot_house ON fa.lot(house_id);
CREATE INDEX idx_daily_sheet_lot_date ON fa.daily_sheet(lot_id, date);
CREATE INDEX idx_delivery_lot_date ON fa.farm_delivery(lot_id, dispatch_date);
