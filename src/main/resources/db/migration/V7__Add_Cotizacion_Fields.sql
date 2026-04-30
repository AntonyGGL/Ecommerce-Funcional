ALTER TABLE orders ADD COLUMN IF NOT EXISTS cotizacion_code VARCHAR(30);
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_cotizacion_code
  ON orders(cotizacion_code) WHERE cotizacion_code IS NOT NULL;
