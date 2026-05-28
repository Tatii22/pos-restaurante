package com.pos.dto.turno;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TurnoCierreDTO(

        @NotNull(message = "El efectivo físico contado es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El efectivo físico contado no puede ser negativo")
        BigDecimal efectivoContado,

        @NotNull(message = "Las transferencias verificadas son obligatorias")
        @DecimalMin(value = "0.0", inclusive = true, message = "Las transferencias verificadas no pueden ser negativas")
        BigDecimal transferenciasVerificadas,

        /**
         * Justificación del cajero cuando la diferencia supera el umbral permitido.
         * El backend la exige automáticamente si |diferenciaTotal| > umbral.
         * Opcional si la caja cuadra dentro del margen.
         */
        @Size(max = 500, message = "La observación no puede superar 500 caracteres")
        String observacionCierre
) {
}
