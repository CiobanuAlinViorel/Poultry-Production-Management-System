CREATE TABLE IF NOT EXISTS slaughterhouse.slaughterhouse (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255),
    address TEXT,
    county VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    daily_capacity_birds INT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS slaughterhouse.line (
    id UUID PRIMARY KEY,
    slaughterhouse_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255),
    speed_bph INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS slaughterhouse.slaughter_session (
    id UUID PRIMARY KEY,
    slaughterhouse_id UUID NOT NULL,
    line_id UUID,
    session_code VARCHAR(64) NOT NULL UNIQUE,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    birds_planned INT,
    birds_processed INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS slaughterhouse.customer (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255),
    vat_no VARCHAR(64),
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS slaughterhouse.sales_order (
    id UUID PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    order_date TIMESTAMPTZ,
    requested_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS slaughterhouse.dispatch_order (
    id UUID PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    sales_order_id UUID,
    customer_id UUID NOT NULL,
    dispatch_date TIMESTAMPTZ,
    quantity INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_line_slaughterhouse ON slaughterhouse.line (slaughterhouse_id);
CREATE INDEX IF NOT EXISTS idx_session_slaughterhouse ON slaughterhouse.slaughter_session (slaughterhouse_id);
