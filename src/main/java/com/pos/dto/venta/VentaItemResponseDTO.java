package com.pos.dto.venta;

import java.math.BigDecimal;

public record VentaItemResponseDTO(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String observacion
) {
}
