package com.pos.dto.turno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TurnoCierreDTO(

        @NotNull
        @PositiveOrZero
        BigDecimal montoFinal
) {
}
