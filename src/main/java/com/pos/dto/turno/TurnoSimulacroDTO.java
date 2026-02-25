package com.pos.dto.turno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TurnoSimulacroDTO(

        @NotNull(message = "El efectivo contado es obligatorio")
        @PositiveOrZero(message = "El efectivo contado no puede ser negativo")
        BigDecimal efectivoContado
) {
}
