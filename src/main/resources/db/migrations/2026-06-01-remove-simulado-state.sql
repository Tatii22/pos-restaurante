-- ====================================================================
-- Migration: 2026-06-01-remove-simulado-state.sql
-- Purpose: Remove SIMULADO state from database after eliminating it from codebase
-- ====================================================================

-- Verificación previa (comentada para auditoría)
-- SELECT COUNT(*) as total_simulado FROM turnos_caja WHERE estado = 'SIMULADO';

-- Cambiar cualquier turno con estado SIMULADO a CERRADO (para preservar datos)
-- Nota: Según auditoría AUDITORIA_SIMULADO.md, debería haber 0 registros
-- pero se hace por seguridad en case de inconsistencias
UPDATE turnos_caja 
SET estado = 'CERRADO' 
WHERE estado = 'SIMULADO';

-- Verificación post-migration
-- SELECT COUNT(*) as remaining_simulado FROM turnos_caja WHERE estado = 'SIMULADO';
-- SELECT DISTINCT estado FROM turnos_caja;
