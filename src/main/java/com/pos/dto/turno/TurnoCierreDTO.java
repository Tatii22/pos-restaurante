package com.pos.dto.turno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record TurnoCierreDTO(

        @NotNull(message = "El monto final es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto final debe ser mayor a 0")
        BigDecimal montoFinal
) {
}
