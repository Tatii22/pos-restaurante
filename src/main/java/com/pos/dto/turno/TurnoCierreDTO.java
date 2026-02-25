package com.pos.dto.turno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TurnoCierreDTO(

        @NotNull(message = "El monto final es obligatorio")
        @PositiveOrZero(message = "El monto final no puede ser negativo")
        BigDecimal montoFinal
) {
}
