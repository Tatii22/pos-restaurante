package com.pos.dto.fiado;

import java.math.BigDecimal;

/**
 * DTO ligero para búsqueda rápida de clientes (autocomplete).
 * Contiene solo la información necesaria para el autocomplete y selección en
 * frontend.
 * 
 * Diseñado para ser rápido en queries y transferencia de datos,
 * sin cargar detalles innecesarios como notas activo, etc.
 */
public record ClienteSearchDTO(
        Long id,
        String nombre,
        String telefono,
        String direccionPredeterminada,
        BigDecimal deudaActual,
        Boolean tieneDeuda) {
}
