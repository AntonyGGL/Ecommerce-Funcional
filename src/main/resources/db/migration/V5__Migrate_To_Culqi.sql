-- V5: Renombrar columna payment_intent_id a culqi_charge_id
-- Migración de Stripe a Culqi como pasarela de pago
ALTER TABLE orders RENAME COLUMN payment_intent_id TO culqi_charge_id;
