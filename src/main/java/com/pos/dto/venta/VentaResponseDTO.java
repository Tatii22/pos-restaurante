package com.pos.dto.venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResponseDTO(
        Long id,
        LocalDateTime fecha,
        String clienteNombre,
        BigDecimal total,
        String formaPago
) {}

