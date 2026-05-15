package com.pos.dto.fiado;

import java.math.BigDecimal;

public record DeudorResponseDTO(
        Long id,
        String nombre,
        String telefono,
        Boolean activo,
        BigDecimal deudaTotal,
        long ventasPendientes
) {
}
