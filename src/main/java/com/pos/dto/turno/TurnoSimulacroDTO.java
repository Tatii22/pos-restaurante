package com.pos.dto.turno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record TurnoSimulacroDTO(

        @NotNull(message = "El efectivo contado es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El efectivo contado debe ser mayor a 0")
        BigDecimal efectivoContado,

        @NotNull(message = "Las transferencias verificadas son obligatorias")
        @DecimalMin(value = "0.0", inclusive = true, message = "Las transferencias verificadas no pueden ser negativas")
        BigDecimal transferenciasVerificadas
) {
}
