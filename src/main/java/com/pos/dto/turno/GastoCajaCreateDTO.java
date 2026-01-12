package com.pos.dto.turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


import java.math.BigDecimal;

public record GastoCajaCreateDTO(

        @NotBlank
        String descripcion,

        @NotNull
        @Positive
        BigDecimal monto,

        @NotNull
        Long tipoGastoId
) {
}
