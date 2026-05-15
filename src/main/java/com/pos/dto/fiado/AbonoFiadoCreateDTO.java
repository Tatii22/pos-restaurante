package com.pos.dto.fiado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AbonoFiadoCreateDTO(
        @NotNull(message = "El deudor es obligatorio")
        Long deudorId,
        @PositiveOrZero(message = "El monto en efectivo no puede ser negativo")
        BigDecimal montoEfectivo,
        @PositiveOrZero(message = "El monto por transferencia no puede ser negativo")
        BigDecimal montoTransferencia,
        String observacion
) {
}
