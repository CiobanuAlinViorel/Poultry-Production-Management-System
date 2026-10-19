-- Read-model entities map these as Double; align column types with the JPA mapping.
ALTER TABLE reporting.lot_fact ALTER COLUMN mortality_pct TYPE DOUBLE PRECISION;
ALTER TABLE reporting.lot_fact ALTER COLUMN avg_weight_g TYPE DOUBLE PRECISION;
ALTER TABLE reporting.lot_fact ALTER COLUMN fcr TYPE DOUBLE PRECISION;
ALTER TABLE reporting.kpi_daily ALTER COLUMN value TYPE DOUBLE PRECISION;
