package com.pos.dto.gastoAdmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoAdminCreateDTO(

        @NotNull
        LocalDate fecha,

        @NotBlank
        String descripcion,

        @NotNull
        @Positive
        BigDecimal monto,

        @NotNull
        Long tipoGastoId
) {
}

