package com.pos.dto.gastoAdmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoAdminCreateDTO(

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        BigDecimal monto,

        BigDecimal montoEfectivo,

        BigDecimal montoTransferencia,

        @NotNull(message = "Debes seleccionar un tipo de gasto")
        Long tipoGastoId
) {
}

