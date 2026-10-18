CREATE TABLE IF NOT EXISTS reporting.site_overview (
    site_id UUID PRIMARY KEY,
    site_type VARCHAR(64),
    name VARCHAR(255),
    active_units INT,
    birds INT,
    open_critical_alerts INT,
    open_warning_alerts INT,
    last_activity_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS reporting.lot_fact (
    lot_id UUID PRIMARY KEY,
    lot_code VARCHAR(64),
    farm_id UUID,
    farm_name VARCHAR(255),
    house_id UUID,
    breed_code VARCHAR(32),
    placement_date TIMESTAMPTZ,
    close_date TIMESTAMPTZ,
    initial_birds INT,
    current_birds INT,
    mortality_pct DECIMAL(10,4),
    avg_weight_g DECIMAL(10,2),
    fcr DECIMAL(10,3),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS reporting.kpi_daily (
    site_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    date DATE NOT NULL,
    metric VARCHAR(64) NOT NULL,
    value DECIMAL(15,4),
    PRIMARY KEY (site_id, unit_id, date, metric)
);

CREATE INDEX IF NOT EXISTS idx_lot_farm ON reporting.lot_fact (farm_id);
CREATE INDEX IF NOT EXISTS idx_kpi_metric ON reporting.kpi_daily (site_id, metric, date);
