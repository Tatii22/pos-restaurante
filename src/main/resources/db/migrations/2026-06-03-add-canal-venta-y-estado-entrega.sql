-- Add columns for MESERO sales channel
-- canal_venta: CAJA (existing) or MESERO
-- estado_entrega_caja: PENDIENTE or ENTREGADO (nullable, only for MESERO sales)

ALTER TABLE ventas
    ADD COLUMN canal_venta VARCHAR(20) NULL,
    ADD COLUMN estado_entrega_caja VARCHAR(20) NULL;

-- Backfill historical records: all existing ventas were created by CAJA
UPDATE ventas SET canal_venta = 'CAJA' WHERE canal_venta IS NULL;

-- Make canal_venta NOT NULL after backfill
ALTER TABLE ventas MODIFY COLUMN canal_venta VARCHAR(20) NOT NULL;
