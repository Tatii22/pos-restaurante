package com.pos.dto.inventario;

import java.time.LocalDate;

public record InventarioResponseDTO(
        Long id,
        LocalDate fecha,
        Long productoId,
        String producto,
        Integer stockInicial,
        Integer stockActual,
        Integer stockMinimo,
        Boolean agotado
) {}
