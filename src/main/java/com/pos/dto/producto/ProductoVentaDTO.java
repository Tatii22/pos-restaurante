package com.pos.dto.producto;

import java.math.BigDecimal;


public record ProductoVentaDTO(
        Long id,
        String nombre,
        BigDecimal precio,
        Boolean agotado,
        String categoriaNombre
) {}


