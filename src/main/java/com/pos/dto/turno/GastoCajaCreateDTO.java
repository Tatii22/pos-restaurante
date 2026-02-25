package com.pos.dto.turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


import java.math.BigDecimal;

public record GastoCajaCreateDTO(

        @NotBlank(message = "La descripcion del gasto es obligatoria")
        String descripcion,

        @NotNull(message = "El monto del gasto es obligatorio")
        @Positive(message = "El monto del gasto debe ser mayor a 0")
        BigDecimal monto,

        @NotNull(message = "Debes seleccionar un tipo de gasto")
        Long tipoGastoId
) {
}
