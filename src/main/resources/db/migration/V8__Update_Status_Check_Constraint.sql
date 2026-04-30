-- Actualizar el CHECK constraint de status para incluir COTIZACION e IGNORED
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'COTIZACION', 'IGNORED'));
