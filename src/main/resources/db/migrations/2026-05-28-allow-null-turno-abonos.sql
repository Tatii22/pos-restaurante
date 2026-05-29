-- Hacer que turno_id en abonos_fiado acepte NULL para permitir
-- que ADMIN registre abonos sin un turno activo.
ALTER TABLE abonos_fiado MODIFY COLUMN turno_id BIGINT NULL;
