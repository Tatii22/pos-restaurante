-- =====================================================================
-- MIGRACIÓN SEGURA: Renombrar modelo conceptual "Deudores" → "Clientes"
-- Fecha: 2026-05-25
-- Motor: MariaDB 10.5+ / MySQL 8+ (InnoDB)
--
-- OBJETIVO
--   Cambiar la entidad base de "deudores" a "clientes frecuentes" sin
--   perder datos, sin romper integridad referencial y manteniendo las
--   relaciones con ventas y abonos_fiado.
--
-- ANTES DE EJECUTAR
--   1. Haz backup completo de la base de datos.
--   2. Ejecuta en una ventana de transacción o con cuidado.
--   3. Obtén los nombres reales de las foreign keys:
--        SHOW CREATE TABLE ventas;
--        SHOW CREATE TABLE abonos_fiado;
--        SHOW CREATE TABLE clientes;   -- (después del RENAME)
--   4. Reemplaza los placeholders FK_VENTAS_DEUDOR y FK_ABONOS_DEUDOR
--      por los nombres reales de tus constraints.
--
-- ORDEN RECOMENDADO (ejecutar paso a paso si prefieres)
-- =====================================================================

START TRANSACTION;

-- 1. Renombrar la tabla principal
RENAME TABLE deudores TO clientes;

-- 2. Actualizar el nombre del índice único (el viejo ya no aplica)
ALTER TABLE clientes
    DROP INDEX IF EXISTS uk_deudor_telefono;

ALTER TABLE clientes
    ADD UNIQUE KEY IF NOT EXISTS uk_cliente_telefono (telefono);

-- =====================================================================
-- 3. TABLA: ventas
--    Cambiar columna deudor_id → cliente_id + recrear FK
-- =====================================================================

-- Reemplaza 'FK_VENTAS_DEUDOR' con el nombre real de la FK actual
ALTER TABLE ventas
    DROP FOREIGN KEY IF EXISTS FK_VENTAS_DEUDOR;

-- Cambiar el nombre de la columna (mantiene datos y nullability)
ALTER TABLE ventas
    CHANGE COLUMN deudor_id cliente_id BIGINT NULL;

-- Recrear la foreign key con el nuevo nombre semántico
ALTER TABLE ventas
    ADD CONSTRAINT fk_ventas_cliente
    FOREIGN KEY (cliente_id)
    REFERENCES clientes (id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- (Opcional) Si existía un índice con nombre viejo, renómbralo:
-- ALTER TABLE ventas DROP INDEX idx_ventas_deudor, ADD INDEX idx_ventas_cliente (cliente_id);

-- =====================================================================
-- 4. TABLA: abonos_fiado
--    Cambiar columna deudor_id → cliente_id + recrear FK
-- =====================================================================

-- Reemplaza 'FK_ABONOS_DEUDOR' con el nombre real de la FK actual
ALTER TABLE abonos_fiado
    DROP FOREIGN KEY IF EXISTS FK_ABONOS_DEUDOR;

ALTER TABLE abonos_fiado
    CHANGE COLUMN deudor_id cliente_id BIGINT NOT NULL;

ALTER TABLE abonos_fiado
    ADD CONSTRAINT fk_abonos_cliente
    FOREIGN KEY (cliente_id)
    REFERENCES clientes (id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- (Opcional) Renombrar índice si aplica
-- ALTER TABLE abonos_fiado DROP INDEX idx_abonos_deudor, ADD INDEX idx_abonos_cliente (cliente_id);

-- =====================================================================
-- 5. VERIFICACIÓN FINAL DENTRO DE LA TRANSACCIÓN
-- =====================================================================
-- SELECT COUNT(*) FROM clientes;
-- SELECT COUNT(*) FROM ventas WHERE cliente_id IS NOT NULL;
-- SELECT COUNT(*) FROM abonos_fiado;

COMMIT;

-- =====================================================================
-- POST-MIGRACIÓN (después de COMMIT)
-- =====================================================================
-- 1. En application-*.yaml / properties pon temporalmente:
--    spring.jpa.hibernate.ddl-auto=validate
--
-- 2. Levanta la aplicación y verifica que no haya errores de mapeo JPA.
--
-- 3. Ejecuta:
--    - ./mvnw compile
--    - Pruebas manuales de Ventas fiado, Abonos, Búsqueda de clientes
--    - npm run typecheck && npm run build (en frontend)
--
-- 4. Cuando todo esté estable, puedes eliminar los endpoints legacy
--    marcados con @Deprecated en FiadoController.
--
-- 5. (Opcional) Limpia índices o constraints antiguos que ya no uses.
-- =====================================================================
