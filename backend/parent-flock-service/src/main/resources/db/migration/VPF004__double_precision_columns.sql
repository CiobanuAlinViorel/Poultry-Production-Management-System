-- Entities map these as Double; align column types with the JPA mapping.
ALTER TABLE pf.house ALTER COLUMN area_m2 TYPE DOUBLE PRECISION;
ALTER TABLE pf.daily_production_sheet ALTER COLUMN feed_kg TYPE DOUBLE PRECISION;
ALTER TABLE pf.daily_production_sheet ALTER COLUMN water_liters TYPE DOUBLE PRECISION;
