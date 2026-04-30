-- V9: Remove culqi_charge_id column (no longer using any online payment gateway)
ALTER TABLE orders DROP COLUMN IF EXISTS culqi_charge_id;
