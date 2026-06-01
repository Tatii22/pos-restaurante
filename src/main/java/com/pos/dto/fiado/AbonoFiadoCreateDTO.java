package com.pos.dto.fiado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AbonoFiadoCreateDTO(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,
        @PositiveOrZero(message = "El monto en efectivo no puede ser negativo")
        BigDecimal montoEfectivo,
        @PositiveOrZero(message = "El monto por transferencia no puede ser negativo")
        BigDecimal montoTransferencia,
        @Size(max = 255, message = "La observacion no puede superar 255 caracteres")
        String observacion
) {
}
