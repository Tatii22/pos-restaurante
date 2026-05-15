package com.pos.dto.fiado;

import com.pos.entity.FormaPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AbonoFiadoResponseDTO(
        Long id,
        LocalDateTime fecha,
        BigDecimal monto,
        BigDecimal montoEfectivo,
        BigDecimal montoTransferencia,
        FormaPago formaPago,
        String observacion,
        String usuario,
        Long turnoId
) {
}
