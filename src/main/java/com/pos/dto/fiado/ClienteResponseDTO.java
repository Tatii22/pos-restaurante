package com.pos.dto.fiado;

import java.math.BigDecimal;

public record ClienteResponseDTO(
        Long id,
        String nombre,
        String telefono,
        String direccionPredeterminada,
        String notas,
        Boolean activo,
        Boolean esDeudor,
        BigDecimal deudaTotal,
        long ventasPendientes
) {
}
