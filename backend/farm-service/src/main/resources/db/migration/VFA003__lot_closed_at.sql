ALTER TABLE fa.lot ADD COLUMN closed_at TIMESTAMPTZ;
CREATE INDEX idx_lot_closed_at ON fa.lot(closed_at);
