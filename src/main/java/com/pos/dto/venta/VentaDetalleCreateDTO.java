package com.pos.dto.venta;

public record VentaDetalleCreateDTO(
        Long productoId,
        Integer cantidad,
        String observacion
) {}
